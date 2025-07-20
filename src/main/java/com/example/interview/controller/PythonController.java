package com.example.interview.controller;

import com.example.interview.service.LargeModelService;
import com.example.interview.service.PythonScriptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/python")
public class PythonController {

    @Autowired
    private PythonScriptService pythonScriptService;

    @Autowired
    private LargeModelService largeModelService; // 假设你已有

    @PostMapping("/analyze")
    public ResponseEntity<?> analyze(@RequestParam String videoPath) {
        try {
            String result = pythonScriptService.runAllScripts(videoPath, null);
            //String modelResponse = largeModelService.sendToModel(result);
            //return ResponseEntity.ok(modelResponse);
            // Python脚本执行完成
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }
}