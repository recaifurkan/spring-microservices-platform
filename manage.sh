#!/usr/bin/env zsh
# ================================================================
#  manage.sh — Spring Microservice Demo Management Tool
#
#  USAGE:
#    ./manage.sh start                              Start everything (auto-downloads OTel agent if missing)
#    ./manage.sh start --env local                 Start with PostgreSQL
#    ./manage.sh start --env test                  Start with the test profile
#    ./manage.sh start --no-build                  Start without building
#    ./manage.sh start <service|index>             Start a single service
#    ./manage.sh start <service|index> --env local  Single service, local env
#    ./manage.sh start <service|index> --no-build   Single service, no build
#    ./manage.sh stop                              Stop everything
#    ./manage.sh stop  <service|index>             Stop a single service
#    ./manage.sh restart                           Restart everything (including build)
#    ./manage.sh restart <service|index>           Restart a single service
#    ./manage.sh restart <service|index> --no-build Restart without building
#    ./manage.sh build                             Build all modules
#    ./manage.sh build <service|index>             Build a single module
#    ./manage.sh status                            Show the status of all services
#    ./manage.sh logs <service|index>              Follow live logs
#    ./manage.sh env up                            Start Docker Compose (PostgreSQL)
#    ./manage.sh env down                          Stop Docker Compose
#    ./manage.sh env down --volumes                Stop Docker Compose + delete volumes
#    ./manage.sh env ps                            Show Docker Compose service status
#    ./manage.sh env logs                          Show Docker Compose logs
#
#  ENVIRONMENTS (--env):
#    default   Embedded H2 database (default, no Docker required)
#    local     PostgreSQL — docker-compose.yml (postgres profile)
#    test      Test profile — separate test configuration
# ================================================================

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
LOG_DIR="$ROOT_DIR/logs"
mkdir -p "$LOG_DIR"

OTEL_JAVA_AGENT_URL="https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v2.28.1/opentelemetry-javaagent.jar"
OTEL_JAVA_AGENT_PATH="$ROOT_DIR/opentelemetry-javaagent.jar"

# ── Colors ───────────────────────────────────────────────────────
GREEN='\033[0;32m'; YELLOW='\033[1;33m'; RED='\033[0;31m'
CYAN='\033[0;36m'; BOLD='\033[1m'; DIM='\033[2m'; MAGENTA='\033[0;35m'; NC='\033[0m'

log()  { print -P "${CYAN}[$(date '+%H:%M:%S')]${NC} $*"; }
ok()   { print -P "${GREEN}  ✔${NC} $*"; }
warn() { print -P "${YELLOW}  !${NC} $*"; }
err()  { print -P "${RED}  ✘${NC} $*" >&2; }
title(){ print -P "\n${BOLD}$*${NC}"; }

# ── Service Definitions ──────────────────────────────────────────
typeset -A SERVICE_PORT=(
  [config-server]=8888
  [discovery-server]=8761
  [auth-server]=9000
  [api-gateway]=8090
  [user-service]=8082
  [product-service]=8084
  [order-service]=8085
  [payment-service]=8086
  [notification-service]=8087
  [cart-service]=8088
  [acs-service]=8089
  [frontend-service]=8070
)

ALL_SERVICES=(
  config-server
  discovery-server
  auth-server
  api-gateway
  user-service
  product-service
  order-service
  payment-service
  notification-service
  cart-service
  acs-service
  frontend-service
)


# ── Argument Parsing ─────────────────────────────────────────────
# Usage: after parse_args "$@", ACTIVE_ENV, BUILD, and TARGET_SERVICE are set.
ACTIVE_ENV="test"   # local | test | <any Spring profile>
BUILD="yes"
TARGET_SERVICE=""

resolve_service_token() {
  local token="$1"

  if (( ${+SERVICE_PORT[$token]} )); then
    echo "$token"
    return 0
  fi

  if [[ "$token" == <-> ]]; then
    local idx="$token"
    (( idx >= 0 && idx < ${#ALL_SERVICES[@]} )) || return 1
    echo "${ALL_SERVICES[$((idx + 1))]}"
    return 0
  fi

  return 1
}

parse_args() {
  local prev_arg=""
  local skip_next=0
  for arg in "$@"; do
    if (( skip_next )); then
      skip_next=0
      continue
    fi
    case "$arg" in
      --no-build)  BUILD="no" ;;
      --env)       ;; # process the value in the next iteration
      --env=*)     ACTIVE_ENV="${arg#--env=}" ;;
      *)
        # If the previous arg was --env, the next value is the env name
        if [[ "${prev_arg:-}" == "--env" ]]; then
          ACTIVE_ENV="$arg"
        elif [[ -z "$TARGET_SERVICE" ]]; then
          TARGET_SERVICE="$(resolve_service_token "$arg" 2>/dev/null || true)"
        fi
        ;;
    esac
    prev_arg="$arg"
  done
}

# ── Active Environment Info ──────────────────────────────────────
env_label() {
  case "$ACTIVE_ENV" in
    default) print -P "${DIM}H2 (embedded)${NC}" ;;
    local)   print -P "${GREEN}local${NC} ${DIM}(PostgreSQL · docker-compose)${NC}" ;;
    test)    print -P "${YELLOW}test${NC} ${DIM}(test profile)${NC}" ;;
    *)       print -P "${MAGENTA}${ACTIVE_ENV}${NC} ${DIM}(custom profile)${NC}" ;;
  esac
}

env_spring_profile() {
  [[ "$ACTIVE_ENV" == "default" ]] && echo "" || echo "$ACTIVE_ENV"
}

# ── Helper Functions ─────────────────────────────────────────────

ensure_opentelemetry_javaagent() {
  [[ -s "$OTEL_JAVA_AGENT_PATH" ]] && return 0

  warn "OpenTelemetry Java agent not found, downloading..."
  local tmp_file="${OTEL_JAVA_AGENT_PATH}.download"
  rm -f "$tmp_file"

  if curl -fL --retry 3 --retry-delay 2 -o "$tmp_file" "$OTEL_JAVA_AGENT_URL"; then
    mv "$tmp_file" "$OTEL_JAVA_AGENT_PATH"
    ok "OpenTelemetry Java agent ready: $OTEL_JAVA_AGENT_PATH"
  else
    rm -f "$tmp_file"
    err "OpenTelemetry Java agent could not be downloaded. Please check your network connection."
    return 1
  fi
}

is_valid_service() {
  (( ${+SERVICE_PORT[$1]} ))
}

service_label() {
  local svc="$1"
  print -P "${BOLD}${svc}${NC} ${DIM}(#$(service_index "$svc"))${NC}"
}

service_index() {
  local target="$1" svc i=0
  for svc in "${ALL_SERVICES[@]}"; do
    if [[ "$svc" == "$target" ]]; then
      print -r -- "$i"
      return 0
    fi
    (( i++ ))
  done
  return 1
}

print_service_catalog() {
  local svc idx i=0
  print -P "${BOLD}  #=SERVICE                  PORT${NC}"
  print -P "  ${DIM}--------------------------------------${NC}"
  for svc in "${ALL_SERVICES[@]}"; do
    idx="$i"
    printf "  %2s=%-24s :%-7s\n" "$idx" "$svc" "${SERVICE_PORT[$svc]}"
    (( i++ ))
  done
}

print_service_urls() {
  print -P "${BOLD}Useful URLs${NC}"
  print -P "  ${DIM}Frontend${NC}          → http://localhost:8070"
  print -P "  ${DIM}API Gateway${NC}        → http://localhost:8090"
  print -P "  ${DIM}Auth Server${NC}        → http://localhost:9000"
  print -P "  ${DIM}Discovery Server${NC}   → http://localhost:8761"
}

get_pid() {
  local pid_file="$LOG_DIR/$1.pid"
  [[ -f "$pid_file" ]] || return 1
  local pid
  pid=$(cat "$pid_file")
  kill -0 "$pid" 2>/dev/null || return 1
  echo "$pid"
}

wait_healthy() {
  local svc="$1"
  local port="${SERVICE_PORT[$svc]}"
  local retries=10 i=0
    warn "Waiting for: ${BOLD}${svc}${NC}${YELLOW} :${port} ...${NC}"
  until curl -sf "http://localhost:${port}/actuator/health" 2>/dev/null | grep -q '"status":"UP"'; do
    (( i++ ))
    if (( i >= retries )); then
      err "${svc} :${port} timeout! See: logs/${svc}.log"
      return 1
    fi
    printf "."
    sleep 2
  done
  print ""
  ok "${BOLD}${svc}${NC} UP → http://localhost:${port}"
}

# ── SUMMARY ──────────────────────────────────────────────────────

print_summary() {
  local env_info
  env_info=$(env_label)
  print ""
  print -P "${GREEN}╔══════════════════════════════════════════════════════════════╗${NC}"
  print -P "${GREEN}║           All services started! 🚀                            ║${NC}"
  print -P "${GREEN}╠══════════════════════════════════════════════════════════════╣${NC}"
  print -P "${GREEN}║  Frontend, Auth, Gateway, Cart, ACS, Discovery are ready    ║${NC}"
  print -P "${GREEN}╚══════════════════════════════════════════════════════════════╝${NC}"
  print ""
  print -P "  ${BOLD}Environment:${NC} ${env_info}"
  [[ "$ACTIVE_ENV" == "local" ]] && \
    print -P "  ${DIM}pgAdmin  →  http://localhost:5050  (admin@local.dev / admin)${NC}"
  print ""
  print_service_urls
  print ""
  print_service_catalog
  print ""
  print -P "  ${DIM}To stop:         ./manage.sh stop${NC}"
  print -P "  ${DIM}To check status:  ./manage.sh status${NC}"
  print ""
}

# ── BUILD ────────────────────────────────────────────────────────

do_build_all() {
  title "📦 Building all modules..."
  cd "$ROOT_DIR"
  mvn clean package -DskipTests -q || { err "Build failed!"; exit 1; }
  ok "Build complete."
}

do_build_one() {
  local svc="$1"
  title "📦 Building ${svc}..."
  [[ -d "$ROOT_DIR/$svc" ]] || { err "Directory not found: $ROOT_DIR/$svc"; exit 1; }
  cd "$ROOT_DIR"
  mvn -pl "$svc" clean package -DskipTests -q || { err "${svc} build failed!"; exit 1; }
  ok "${svc} build complete."
}

# ── START ────────────────────────────────────────────────────────

start_one() {
  local svc="$1"
  local dir="$ROOT_DIR/$svc"
  [[ -d "$dir" ]] || { err "Directory not found: $dir"; return 1; }

  ensure_opentelemetry_javaagent || return 1

  local existing
  if existing=$(get_pid "$svc") 2>/dev/null; then
    warn "${svc} is already running (PID=${existing})"
    return 0
  fi

  local profile
  profile=$(env_spring_profile)

  if [[ -n "$profile" ]]; then
    log "Starting: ${BOLD}${svc}${NC} ${DIM}[env=${ACTIVE_ENV}]${NC}"
  else
    log "Starting: ${BOLD}${svc}${NC}"
  fi

  cd "$dir"
  if [[ -n "$profile" ]]; then
    SPRING_PROFILES_ACTIVE="$profile" mvn spring-boot:run -q > "$LOG_DIR/$svc.log" 2>&1 &
  else
    # Clear SPRING_PROFILES_ACTIVE inherited from the shell;
    # otherwise the value from a previous --env run would leak in.
    env -u SPRING_PROFILES_ACTIVE mvn spring-boot:run -q > "$LOG_DIR/$svc.log" 2>&1 &
  fi
  local pid=$!
  echo "$pid" > "$LOG_DIR/$svc.pid"
  cd "$ROOT_DIR"
  ok "${svc} started (PID=${pid})"
}

# ── STOP ─────────────────────────────────────────────────────────

stop_one() {
  local svc="$1"
  local pid
  if pid=$(get_pid "$svc") 2>/dev/null; then
    log "Stopping: ${BOLD}${svc}${NC} (PID=${pid})"
    kill "$pid" 2>/dev/null
    local i=0
    while kill -0 "$pid" 2>/dev/null && (( i < 20 )); do
      sleep 0.5; (( i++ ))
    done
    kill -0 "$pid" 2>/dev/null && kill -9 "$pid" 2>/dev/null
    rm -f "$LOG_DIR/$svc.pid"
    ok "${svc} stopped."
  else
    warn "${svc} is not running."
    rm -f "$LOG_DIR/$svc.pid"
  fi
}

stop_all() {
  title "🛑 Stopping all services in parallel..."

  local pids=()
  for svc in "${ALL_SERVICES[@]}"; do
    (
      pid_file="$LOG_DIR/$svc.pid"
      if running=$(get_pid "$svc") 2>/dev/null; then
        kill "$running" 2>/dev/null
        i=0
        while kill -0 "$running" 2>/dev/null && (( i < 20 )); do
          sleep 0.5; (( i++ ))
        done
        kill -0 "$running" 2>/dev/null && kill -9 "$running" 2>/dev/null
        rm -f "$pid_file"
            print -P "  ${GREEN}✔${NC} ${svc} stopped."
      else
        rm -f "$pid_file"
        print -P "  ${YELLOW}!${NC} ${svc} was not running."
      fi
    ) &
    pids+=($!)
  done

  for p in "${pids[@]}"; do
    wait "$p" 2>/dev/null
  done

  ok "All services stopped."
}

# ── RESTART ──────────────────────────────────────────────────────

restart_one() {
  local svc="$1" build="$2"
  stop_one "$svc"
  sleep 1
  [[ "$build" == "yes" ]] && do_build_one "$svc"
  start_one "$svc"
  wait_healthy "$svc"
}

# ── STATUS ───────────────────────────────────────────────────────

print_status() {
  title "Service Status"
  print ""
  printf "  %-4s %-28s %-8s %-12s %s\n" "#" "SERVICE" "PORT" "STATUS" "PID"
  printf "  %s\n" "------------------------------------------------------"
  local _i=0
  for svc in "${ALL_SERVICES[@]}"; do
    _idx="$_i"
    _port="${SERVICE_PORT[$svc]}"
    _pid="-"
    _rpid=""
    if _rpid=$(get_pid "$svc") 2>/dev/null; then
      _pid="$_rpid"
      if curl -sf "http://localhost:${_port}/actuator/health" 2>/dev/null | grep -q '"status":"UP"'; then
        printf "  %-4s %-28s :%-7s \033[0;32m%-12s\033[0m %s\n" "$_idx" "$svc" "$_port" "UP" "$_pid"
      else
        printf "  %-4s %-28s :%-7s \033[1;33m%-12s\033[0m %s\n" "$_idx" "$svc" "$_port" "STARTING" "$_pid"
      fi
    else
      printf "  %-4s %-28s :%-7s \033[0;31m%-12s\033[0m %s\n" "$_idx" "$svc" "$_port" "DOWN" "$_pid"
    fi
    (( _i++ ))
  done
  print ""

    # Docker Compose status (if installed)
  if command -v docker-compose &>/dev/null || docker compose version &>/dev/null 2>&1; then
    local dc_cmd
    docker compose version &>/dev/null 2>&1 && dc_cmd="docker compose" || dc_cmd="docker-compose"
    print -P "  ${BOLD}🐳 Docker Compose (PostgreSQL):${NC}"
    local pg_status
    pg_status=$(cd "$ROOT_DIR" && $dc_cmd ps --format "table {{.Name}}\t{{.Status}}" 2>/dev/null | grep -v "^NAME" | grep -v "^$" || true)
    if [[ -n "$pg_status" ]]; then
      while IFS= read -r line; do
        print "    $line"
      done <<< "$pg_status"
    else
      print -P "    ${DIM}(not running — start with: ./manage.sh env up)${NC}"
    fi
    print ""
  fi
}

print_service_list() {
  title "Service Index List"
  print ""
  printf "  %-4s %-28s %-8s\n" "#" "SERVICE" "PORT"
  printf "  %s\n" "--------------------------------------"
  local _i=0
  for svc in "${ALL_SERVICES[@]}"; do
    _port="${SERVICE_PORT[$svc]}"
    printf "  %-4s %-28s :%-7s\n" "$_i" "$svc" "$_port"
    (( _i++ ))
  done
  print ""
  print -P "  ${DIM}Use either the service name or its index with start/stop/restart/build/logs.${NC}"
  print ""
}

# ── LOGS ─────────────────────────────────────────────────────────

show_logs() {
  local svc="$1"
  local log_file="$LOG_DIR/$svc.log"
  [[ -f "$log_file" ]] || { err "Log file not found: $log_file"; exit 1; }
  title "📋 ${svc} — Live Logs (Ctrl+C to exit)"
  tail -f "$log_file"
}

# ── ENV (Docker Compose) ─────────────────────────────────────────

# Determine the docker-compose command (v1 or v2)
_dc() {
  if docker compose version &>/dev/null 2>&1; then
    docker compose "$@"
  elif command -v docker-compose &>/dev/null; then
    docker-compose "$@"
  else
    err "docker / docker-compose not found. Is Docker Desktop installed?"
    exit 1
  fi
}

do_env() {
  local sub="${1:-help}"
  shift 2>/dev/null || true

  cd "$ROOT_DIR"

  case "$sub" in
    up)
      title "🐳 Starting Docker Compose (PostgreSQL + pgAdmin + Grafana)..."
      _dc up -d "$@"
      print ""
      ok "PostgreSQL up → localhost:5432  (user: app / password: password)"
      ok "pgAdmin up   → http://localhost:5050  (admin@local.dev / admin)"
      ok "Jaeger UI up → http://localhost:16686  (OTLP: localhost:4318)"
      ok "Grafana up   → http://localhost:3000  (admin / admin)"
      print ""
      print -P "  ${DIM}To start services with the local env:${NC}"
      print -P "  ${DIM}  ./manage.sh start --env local --no-build${NC}"
      ;;
    down)
      title "🛑 Stopping Docker Compose..."
      if [[ "${1:-}" == "--volumes" || "${1:-}" == "-v" ]]; then
        warn "Volumes are also being deleted! All PostgreSQL data will be lost."
        _dc down -v
        ok "Services and volumes deleted."
      else
        _dc down
        ok "Services stopped. (Data preserved — delete with: ./manage.sh env down --volumes)"
      fi
      ;;
    ps|status)
      title "🐳 Docker Compose Service Status"
      _dc ps
      ;;
    logs)
      title "🐳 Docker Compose Logs (Ctrl+C to exit)"
      _dc logs -f "${@:-}"
      ;;
    restart)
      title "🔄 Restarting Docker Compose..."
      _dc restart "$@"
      ok "Docker services restarted."
      ;;
    help|*)
      print -P "
${BOLD}Usage:${NC}  ./manage.sh env <subcommand> [options]

${BOLD}Subcommands:${NC}
  ${GREEN}up${NC}               Start PostgreSQL + pgAdmin
  ${YELLOW}down${NC}             Stop (data preserved)
  ${YELLOW}down --volumes${NC}   Stop + delete all data
  ${CYAN}ps${NC}               Show running container status
  ${CYAN}logs${NC}             Follow container logs
  ${CYAN}restart${NC}          Restart containers

${BOLD}Connection Info:${NC}
  PostgreSQL  →  localhost:5432  (user: app / password: password)
  pgAdmin     →  http://localhost:5050  (admin@local.dev / admin)
  Jaeger UI   →  http://localhost:16686  (distributed tracing)
  OTLP HTTP   →  localhost:4318  ← Spring Boot sends traces here
"
      ;;
  esac
}

# ── HELP ──────────────────────────────────────────────────────────

print_help() {
  print -P "
${BOLD}manage.sh — Spring Microservice Demo Management Tool${NC}

${BOLD}USAGE:${NC}
  ./manage.sh <command> [service] [--env <environment>] [--no-build]

  ${DIM}You can also use a 0-based index instead of a service name: 0, 1, 2 ...${NC}

${BOLD}COMMANDS:${NC}
  ${GREEN}start${NC}                                   Start everything (build included, auto-downloads OTel agent if missing)
  ${GREEN}start${NC}  --no-build                       Start without building
  ${GREEN}start${NC}  --env local                      Start with PostgreSQL
  ${GREEN}start${NC}  --env test                       Start with the test profile
  ${GREEN}start${NC}  <service|index>                  Start a single service
  ${GREEN}start${NC}  <service|index> --env local --no-build  Single service, local env, no build

  ${YELLOW}stop${NC}                                    Stop everything
  ${YELLOW}stop${NC}   <service|index>                  Stop a single service

  ${CYAN}restart${NC}                                 Restart everything (build included)
  ${CYAN}restart${NC}  --no-build                      Restart everything without building
  ${CYAN}restart${NC}  <service|index>                  Restart a single service
  ${CYAN}restart${NC}  <service|index> --env local --no-build Restart a single service, local env, no build

  ${BOLD}build${NC}                                   Build all modules
  ${BOLD}build${NC}    <service|index>                  Build a single module

  ${BOLD}status${NC}                                  Show the status of all services
  ${BOLD}list${NC}                                    Show service indexes and ports
  ${BOLD}logs${NC}     <service|index>                  Follow live logs

  ${MAGENTA}env up${NC}                                  Start Docker Compose (PostgreSQL)
  ${MAGENTA}env down${NC}                                Stop Docker Compose
  ${MAGENTA}env down --volumes${NC}                      Stop + delete data
  ${MAGENTA}env ps${NC}                                  Container status
  ${MAGENTA}env logs${NC}                                Container logs

${BOLD}ENVIRONMENTS (--env):${NC}
  ${DIM}default${NC}   Embedded H2 database (used when no flag is provided)
  ${GREEN}local${NC}     PostgreSQL — first run: ./manage.sh env up
  ${YELLOW}test${NC}      Test Spring profile

${BOLD}SERVICES:${NC}
$(print_service_catalog)

${BOLD}EXAMPLES:${NC}
  ./manage.sh env up                                    # Start PostgreSQL
  ./manage.sh start --env local --no-build              # Start everything with local env
  ./manage.sh start auth-server --env local --no-build  # Single service, local env
  ./manage.sh start 2 --env local --no-build            # Same as auth-server
  ./manage.sh restart frontend-service --no-build       # Restart with H2
  ./manage.sh restart auth-server --env local --no-build
  ./manage.sh stop auth-server
  ./manage.sh build frontend-service
  ./manage.sh build 11                                  # Build frontend-service by index
  ./manage.sh list                                      # Show index-to-service mapping
  ./manage.sh logs frontend-service
  ./manage.sh logs 11                                   # Tail frontend logs by index
  ./manage.sh status
  ./manage.sh env down                                  # Stop Docker only
"
}

# ── MAIN ──────────────────────────────────────────────────────────

COMMAND="${1:-help}"
shift 2>/dev/null || true   # pass the remaining args to parse_args

parse_args "$@"

case "$COMMAND" in

  start)
    if [[ -n "$TARGET_SERVICE" ]]; then
      is_valid_service "$TARGET_SERVICE" || { err "Unknown service: $TARGET_SERVICE"; exit 1; }
      [[ "$BUILD" == "yes" ]] && do_build_one "$TARGET_SERVICE"
      start_one "$TARGET_SERVICE"
      wait_healthy "$TARGET_SERVICE"
    else
      [[ "$BUILD" == "yes" ]] && do_build_all
      title "🚀 Starting services [env=$(env_label)]..."
      for svc in "${ALL_SERVICES[@]}"; do
        start_one "$svc"
        wait_healthy "$svc" || true
      done
      print_summary
    fi
    ;;

  stop)
    if [[ -n "$TARGET_SERVICE" ]]; then
      is_valid_service "$TARGET_SERVICE" || { err "Unknown service: $TARGET_SERVICE"; exit 1; }
      stop_one "$TARGET_SERVICE"
    else
      stop_all
    fi
    ;;

  restart)
    if [[ -n "$TARGET_SERVICE" ]]; then
      is_valid_service "$TARGET_SERVICE" || { err "Unknown service: $TARGET_SERVICE"; exit 1; }
      restart_one "$TARGET_SERVICE" "$BUILD"
    else
      [[ "$BUILD" == "yes" ]] && do_build_all
      stop_all
      sleep 2
      title "🚀 Restarting services [env=$(env_label)]..."
      for svc in "${ALL_SERVICES[@]}"; do
        start_one "$svc"
        wait_healthy "$svc" || true
      done
      print_summary
    fi
    ;;

  build)
    if [[ -n "$TARGET_SERVICE" ]]; then
      is_valid_service "$TARGET_SERVICE" || { err "Unknown service: $TARGET_SERVICE"; exit 1; }
      do_build_one "$TARGET_SERVICE"
    else
      do_build_all
    fi
    ;;

  status)
    print_status
    ;;

  list)
    print_service_list
    ;;

  logs)
    [[ -z "$TARGET_SERVICE" ]] && { err "Usage: ./manage.sh logs <service>"; exit 1; }
    is_valid_service "$TARGET_SERVICE" || { err "Unknown service: $TARGET_SERVICE"; exit 1; }
    show_logs "$TARGET_SERVICE"
    ;;

  env)
    # $@ has already been consumed by parse_args; reuse the original arguments
    # and pass ARG2, ARG3, etc. directly to the env command
    do_env "$@"
    ;;

  help|--help|-h)
    print_help
    ;;

  *)
    err "Unknown command: $COMMAND"
    print_help
    exit 1
    ;;
esac



