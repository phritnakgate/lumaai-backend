package org.bkkz.lumabackend.service;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.JasperRunManager;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;
import net.sf.jasperreports.engine.util.JRLoader;
import org.bkkz.lumabackend.utils.ThaiMonth;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class FormService {

    public byte[] getMisTaskReport(String reportYearMonth, List<Map<String, Object>> tasks) {
        try {
            Map<String, Object> parameterMap = new HashMap<>();
            int targetYr = Integer.parseInt(reportYearMonth.substring(0, 4));
            String inputMonthToThai = ThaiMonth.toThaiName(reportYearMonth.substring(reportYearMonth.length() - 2)) + " " + (targetYr + 543);

            Map<Integer, Integer> weeklyCounts = tasks.stream()
                    .map(task -> task.get("dateTime"))
                    .filter(obj -> obj instanceof String)
                    .map(dateStr -> ZonedDateTime.parse((String) dateStr))
                    .collect(Collectors.groupingBy(
                            zdt -> (zdt.getDayOfMonth() - 1) / 7 + 1,
                            Collectors.summingInt(zdt -> 1)
                    ));

            // Convert dateTime strings to java.util.Date
            tasks.forEach(task -> {
                Object dateTimeObj = task.get("dateTime");
                if (dateTimeObj instanceof String) {
                    ZonedDateTime zdt = ZonedDateTime.parse((String) dateTimeObj);
                    task.put("dateTime", java.util.Date.from(zdt.toInstant()));

                }
            });
            System.out.println(tasks);
            JRMapCollectionDataSource taskDataSource = new JRMapCollectionDataSource((Collection<Map<String, ?>>)(Collection<?>) tasks);

            parameterMap.put("task_report_date", inputMonthToThai);
            parameterMap.put("total_tasks", tasks.size());
            parameterMap.put("total_tasks_finished",(int) tasks.stream().filter(task -> Boolean.TRUE.equals(task.get("isFinished"))).count());
            parameterMap.put("total_task_w1", weeklyCounts.getOrDefault(1, 0));
            parameterMap.put("total_task_w2", weeklyCounts.getOrDefault(2, 0));
            parameterMap.put("total_task_w3", weeklyCounts.getOrDefault(3, 0));
            parameterMap.put("total_task_w4", weeklyCounts.getOrDefault(4, 0));
            parameterMap.put("total_task_w5", weeklyCounts.getOrDefault(5, 0));
            parameterMap.put("task_monthly_list", taskDataSource);

            return generateReport("reports/mis_task_report.jasper", parameterMap, taskDataSource);

        } catch (Exception e) {
            System.out.println("Error generating MIS Task Report: " + e.getMessage() + e.getCause());
            throw new RuntimeException(e);
        }
    }

    private static byte[] generateReport(String reportFile, Map<String, Object> list, JRDataSource dataSource) throws Exception {
        ClassPathResource resource = new ClassPathResource(reportFile);
        try (InputStream inputStream = resource.getInputStream()) {
            JasperReport jasperReport = (JasperReport) JRLoader.loadObject(inputStream);
            return JasperRunManager.runReportToPdf(jasperReport, list, dataSource);
        }
    }

}
