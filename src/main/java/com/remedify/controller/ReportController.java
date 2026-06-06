package com.remedify.controller;

import com.remedify.repository.ScanReportRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/scans/{scanId}/report")
public class ReportController {

  private final ScanReportRepository reportRepository;

  public ReportController(ScanReportRepository reportRepository) {
    this.reportRepository = reportRepository;
  }

  @GetMapping(produces = MediaType.TEXT_HTML_VALUE)
  public ResponseEntity<String> getHtmlReport(@PathVariable UUID scanId) {
    log.debug("Fetching HTML report for scan: {}", scanId);
    return reportRepository.findByScanId(scanId)
        .map(report -> ResponseEntity.ok(report.getHtmlReport()))
        .orElse(ResponseEntity.notFound().build());
  }

  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<String> getJsonReport(@PathVariable UUID scanId) {
    log.debug("Fetching JSON report for scan: {}", scanId);
    return reportRepository.findByScanId(scanId)
        .map(report -> ResponseEntity.ok(report.getJsonReport()))
        .orElse(ResponseEntity.notFound().build());
  }

  @GetMapping("/download")
  public ResponseEntity<String> downloadReport(
      @PathVariable UUID scanId,
      @RequestParam(defaultValue = "html") String format) {
    log.debug("Downloading {} report for scan: {}", format, scanId);

    return reportRepository.findByScanId(scanId)
        .map(report -> {
          String content = "html".equalsIgnoreCase(format) ?
              report.getHtmlReport() :
              report.getJsonReport();
          String mediaType = "html".equalsIgnoreCase(format) ?
              "text/html" :
              "application/json";
          String filename = "remedify-report-" + scanId + "." + format;

          return ResponseEntity.ok()
              .contentType(MediaType.parseMediaType(mediaType))
              .header(HttpHeaders.CONTENT_DISPOSITION,
                  ContentDisposition.attachment().filename(filename).build().toString())
              .body(content);
        })
        .orElse(ResponseEntity.notFound().build());
  }
}
