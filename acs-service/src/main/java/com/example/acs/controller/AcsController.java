package com.example.acs.controller;

import com.example.acs.dto.AcsInitRequest;
import com.example.acs.dto.AcsInitResponse;
import com.example.acs.model.AcsTransaction;
import com.example.acs.service.AcsService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Controller
@RequestMapping("/acs")
public class AcsController {

    private final AcsService service;
    public AcsController(AcsService service) { this.service = service; }

    /** payment-service → create a new transaction record */
    @PostMapping("/init")
    @ResponseBody
    public AcsInitResponse initTransaction(@RequestBody AcsInitRequest req) {
        AcsTransaction txn = service.createTransaction(
                req.paymentId(),
                req.paymentCallbackUrl(),
                req.frontendCallbackUrl(),
                req.amount(),
                req.merchantName()
        );
        return new AcsInitResponse(txn.getTransactionId());
    }

    /** Browser: Challenge OTP page */
    @GetMapping("/challenge/{transactionId}")
    public String challengePage(@PathVariable String transactionId, Model model) {
        try {
            AcsTransaction txn = service.getTransaction(transactionId);
            model.addAttribute("txn", txn);
            model.addAttribute("error", null);
        } catch (Exception e) {
            model.addAttribute("error", "Invalid or expired transaction.");
        }
        return "challenge";
    }

    /** OTP form submit — /challenge/{id} (form action fallback) */
    @PostMapping("/challenge/{transactionId}")
    public String verifyOtpDirect(@PathVariable String transactionId,
                                  @RequestParam String otp, Model model) {
        return verifyOtp(transactionId, otp, model);
    }

    /** OTP form submit — /challenge/{id}/verify */
    @PostMapping("/challenge/{transactionId}/verify")
    public String verifyOtp(@PathVariable String transactionId,
                            @RequestParam String otp, Model model) {
        try {
            AcsTransaction txn = service.verifyOtp(transactionId, otp);
            if (txn.getStatus() == AcsTransaction.TxnStatus.SUCCESS) {
                return "redirect:/acs/result/" + transactionId + "?status=SUCCESS";
            } else if (txn.getStatus() == AcsTransaction.TxnStatus.FAILED) {
                return "redirect:/acs/result/" + transactionId + "?status=FAILED";
            } else {
                // OTP is wrong, but attempts remain
                model.addAttribute("txn", txn);
                model.addAttribute("error", "Invalid OTP! You have " + (3 - txn.getAttempts()) + " attempts left.");
                return "challenge";
            }
        } catch (Exception e) {
            model.addAttribute("txn", null);
            model.addAttribute("error", e.getMessage());
            return "challenge";
        }
    }

    /** Result page → redirect the user to the frontend */
    @GetMapping("/result/{transactionId}")
    public String resultPage(@PathVariable String transactionId,
                             @RequestParam String status, Model model) {
        try {
            AcsTransaction txn = service.getTransaction(transactionId);
            String redirectUrl = txn.getFrontendCallbackUrl();
            if (redirectUrl != null) {
                String separator = redirectUrl.contains("?") ? "&" : "?";
                return "redirect:" + redirectUrl + separator + "paymentId=" + txn.getPaymentId() + "&status=" + status;
            }
            model.addAttribute("status", status);
            model.addAttribute("txn", txn);
        } catch (Exception e) {
            model.addAttribute("status", status);
            model.addAttribute("txn", null);
        }
        return "result";
    }

    @GetMapping("/health")
    @ResponseBody
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "acs-service"));
    }
}

