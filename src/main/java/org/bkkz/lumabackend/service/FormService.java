package org.bkkz.lumabackend.service;

import com.google.api.gax.paging.Page;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.Storage;
import com.google.firebase.cloud.StorageClient;
import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JREmptyDataSource;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.JasperRunManager;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;
import net.sf.jasperreports.engine.util.JRLoader;
import org.bkkz.lumabackend.utils.TaskCategory;
import org.bkkz.lumabackend.utils.ThaiMonth;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.net.URL;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class FormService {

    private final Client vertexAIClient;

    @Autowired
    public FormService(Client vertexAIClient) {
        this.vertexAIClient = vertexAIClient;
    }

    static class CategoryStats{
        int task_category_all = 0;
        int task_category_finished = 0;
    }

    private final int BLOB_SIGNED_URL_EXPIRATION_MINUTES = 60;

    public byte[] getMonthlyTaskReport(String reportYearMonth, List<Map<String, Object>> tasks) {
        try {
            Map<String, Object> parameterMap = new HashMap<>();
            Map<String, Object> dataToAnalyze = new HashMap<>();

            //Report Title
            int targetYr = Integer.parseInt(reportYearMonth.substring(0, 4));
            String inputMonthToThai = ThaiMonth.toThaiName(reportYearMonth.substring(reportYearMonth.length() - 2)) + " " + (targetYr + 543);

            //Sum Task By Week
            Map<Integer, Integer> weeklyCounts = tasks.stream()
                    .map(task -> task.get("dateTime"))
                    .filter(obj -> obj instanceof String)
                    .map(dateStr -> ZonedDateTime.parse((String) dateStr))
                    .collect(Collectors.groupingBy(
                            zdt -> (zdt.getDayOfMonth() - 1) / 7 + 1,
                            Collectors.summingInt(zdt -> 1)
                    ));

            // Convert dateTime strings to java.util.Date and convert int to Integer
            tasks.forEach(task -> {
                Object dateTimeObj = task.get("dateTime");
                if (dateTimeObj instanceof String) {
                    ZonedDateTime zdt = ZonedDateTime.parse((String) dateTimeObj);
                    task.put("dateTime", java.util.Date.from(zdt.toInstant()));
                }
                Object cat = task.get("category");
                if (cat instanceof Number) {
                    task.put("category", ((Number) cat).intValue());
                } else {
                    try {
                        task.put("category", Integer.parseInt(cat.toString()));
                    } catch (NumberFormatException ex) {
                        task.put("category", 0);
                    }
                }

                Object pr = task.get("priority");
                if (pr instanceof Number) {
                    task.put("priority", ((Number) pr).intValue());
                } else {
                    try {
                        task.put("priority", Integer.parseInt(pr.toString()));
                    } catch (NumberFormatException ex) {
                        task.put("priority", 0);
                    }
                }

            });
            System.out.println(tasks);
            JRMapCollectionDataSource taskDataSource = new JRMapCollectionDataSource((Collection<Map<String, ?>>)(Collection<?>) tasks);

            //Separate By Work Category & Count Priority
            int total_task_p_high = 0;
            int total_task_p_low = 0;
            int total_task_p_med = 0;

            Map<Integer, CategoryStats> categoryStatsMap = new HashMap<>();
            for (Map<String, Object> task : tasks) {
                Integer categoryId = ((Number) task.get("category")).intValue();
                Boolean isFinished = (Boolean) task.get("isFinished");
                int priority = ((Number) task.get("priority")).intValue();
                CategoryStats stats = categoryStatsMap.computeIfAbsent(categoryId, k -> new CategoryStats());
                stats.task_category_all++;
                if (isFinished) {
                    switch (priority) {
                        case 0: total_task_p_high += 1; break;
                        case 1: total_task_p_med += 1; break;
                        case 2: total_task_p_low += 1; break;
                    }
                    stats.task_category_finished++;
                }
            }
            //System.out.println(categoryStatsMap);
            parameterMap.put("total_task_finish_t0", 0);
            parameterMap.put("total_task_finish_t1", 0);
            parameterMap.put("total_task_finish_t2", 0);
            parameterMap.put("total_task_finish_t3", 0);
            parameterMap.put("total_task_finish_t4", 0);
            List<Map<String, Object>> taskByCategory = new ArrayList<>();
            for (Map.Entry<Integer, CategoryStats> entry : categoryStatsMap.entrySet()) {
                Integer categoryId = entry.getKey();
                CategoryStats stats = entry.getValue();

                Map<String, Object> row = new HashMap<>();
                String param = "total_task_finish_t" + categoryId;
                parameterMap.put(param, stats.task_category_finished);
                row.put("task_category_title", TaskCategory.getName(categoryId));
                row.put("task_category_all", stats.task_category_all);
                row.put("task_category_finished", stats.task_category_finished);

                taskByCategory.add(row);
            }

            System.out.println(taskByCategory);
            System.out.println(parameterMap);
            JRMapCollectionDataSource taskByCategoryDataSource = new JRMapCollectionDataSource((Collection<Map<String, ?>>)(Collection<?>) taskByCategory);

            InputStream ic = Thread.currentThread().getContextClassLoader().getResourceAsStream("images/ic_idea.png");

            int completedTasks = (int) tasks.stream().filter(task -> Boolean.TRUE.equals(task.get("isFinished"))).count();

            //Analyze Report Data with Vertex AI
            dataToAnalyze.put("reportMonth", inputMonthToThai);
            dataToAnalyze.put("overallStats", Map.of(
                    "totalTasks", tasks.size(),
                    "completedTasks", completedTasks,
                    "completionRate", tasks.isEmpty() ? 0 : (completedTasks * 100 / tasks.size())
            ));
            dataToAnalyze.put("completedByPriority", Map.of(
                    "high", total_task_p_high,
                    "medium", total_task_p_med,
                    "low", total_task_p_low
            ));
            dataToAnalyze.put("completedByWeek", weeklyCounts);
            dataToAnalyze.put("completedByCategory", taskByCategory);

            System.out.println(dataToAnalyze);

            parameterMap.put("task_report_month", inputMonthToThai);
            parameterMap.put("total_tasks", tasks.size());
            parameterMap.put("total_tasks_finished",completedTasks);
            parameterMap.put("total_task_w1", weeklyCounts.getOrDefault(1, 0));
            parameterMap.put("total_task_w2", weeklyCounts.getOrDefault(2, 0));
            parameterMap.put("total_task_w3", weeklyCounts.getOrDefault(3, 0));
            parameterMap.put("total_task_w4", weeklyCounts.getOrDefault(4, 0));
            parameterMap.put("total_task_w5", weeklyCounts.getOrDefault(5, 0));
            parameterMap.put("task_by_category", taskByCategoryDataSource);
            parameterMap.put("total_task_p_high", total_task_p_high);
            parameterMap.put("total_task_p_med", total_task_p_med);
            parameterMap.put("total_task_p_low", total_task_p_low);
            parameterMap.put("task_monthly_list", taskDataSource);
            parameterMap.put("icon_analyzed", ic);
            parameterMap.put("task_analyze",analyzeReportData(dataToAnalyze));

            return generateReport("reports/mis_task_report_monthly_p1.jasper", parameterMap, new JREmptyDataSource());

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

    public String uploadPdfFile(String uid, InputStream fileStream, String reportType, String fileName) {
        Bucket bucket = StorageClient.getInstance().bucket();

        String fullFileName = reportType + "_" + fileName;
        String objectPath = "users/" + uid + "/"+ reportType + "/" + fullFileName;

        Blob blob = bucket.create(objectPath, fileStream, "application/pdf");

        return blob.signUrl(BLOB_SIGNED_URL_EXPIRATION_MINUTES, TimeUnit.MINUTES, Storage.SignUrlOption.withV4Signature()).toString();
    }

    public List<Map<String, Object>> getAllUserReports(String uid, String reportType) {
        Bucket bucket = StorageClient.getInstance().bucket();
        String prefix = "users/" + uid + "/" + reportType + "/";
        List<Map<String, Object>> fileList = new ArrayList<>();
        try{
            Page<Blob> blobs = bucket.list(Storage.BlobListOption.prefix(prefix));
            for (Blob blob : blobs.iterateAll()) {
                String fullPath = blob.getName();
                String fileName = fullPath.substring(prefix.length());

                if (fileName.isEmpty()) {
                    continue;
                }

                URL signedUrl = blob.signUrl(BLOB_SIGNED_URL_EXPIRATION_MINUTES, TimeUnit.MINUTES, Storage.SignUrlOption.withV4Signature());

                Map<String, Object> fileData = new HashMap<>();
                fileData.put("fileName", fileName);
                fileData.put("url", signedUrl.toString());
                fileData.put("lastModified", blob.getUpdateTimeOffsetDateTime());

                fileList.add(fileData);

                fileList.sort((m1, m2) -> {
                    OffsetDateTime time1 = (OffsetDateTime) m1.get("lastModified");
                    OffsetDateTime time2 = (OffsetDateTime) m2.get("lastModified");
                    return time2.compareTo(time1);
                });
            }

        }catch(Exception e){
            return null;
        }
        return fileList;
    }

    public void deleteUserReport(String filePath){
        try{
            Bucket bucket = StorageClient.getInstance().bucket();
            Blob blob = bucket.get(filePath);
            if (blob != null) {
                blob.delete();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String analyzeReportData(Map<String, Object> dataToAnalyze){

        try{
            String prompt = "คุณคือ AI ผู้ช่วยวิเคราะห์ประสิทธิภาพการทำงานที่มีความเชี่ยวชาญ\n\n" +
                    "จากข้อมูลสรุปการทำงานประจำเดือนในรูปแบบ JSON ต่อไปนี้:\n" +
                    dataToAnalyze + "\n\n" +
                    "จงทำการวิเคราะห์การทำงานของผู้ใช้ในเดือนนี้อย่างละเอียด โดยแบ่งการวิเคราะห์ออกเป็นหัวข้อต่อไปนี้ แล้วสรุปมาเป็นย่อหน้าเดียวความยาวไม่เกิน 4 บรรทัด A4 โดยแทนผู้ใช้เป็น คุณ ในตอนตอบกลับ:\n\n" +
                    "1. **สรุปภาพรวม (Overall Summary):** สรุปผลการทำงานโดยรวม บอกถึงจุดเด่นที่สำคัญที่สุดในเดือนนี้\n\n" +
                    "2. **การวิเคราะห์เชิงลึก (In-depth Analysis):**\n" +
                    "    * **ด้านปริมาณและประสิทธิภาพ:** วิเคราะห์จากอัตราส่วนของงานที่ทำสำเร็จ (Completion Rate) เทียบกับงานทั้งหมด ชี้ให้เห็นว่าประสิทธิภาพโดยรวมอยู่ในระดับใด\n" +
                    "    * **ด้านการโฟกัสประเภทงาน:** งานประเภทไหนที่ผู้ใช้ทำสำเร็จมากที่สุด และมีงานประเภทไหนที่อาจถูกละเลยหรือไม่สามารถปิดงานได้ (เช่น POC, อบรม)\n" +
                    "    * **ด้านการจัดลำดับความสำคัญ:** ผู้ใช้สามารถทำงานที่มีความเร่งด่วนสูง (High Priority) ได้ดีเพียงใด สัดส่วนงานสำคัญที่ทำสำเร็จเป็นอย่างไร\n" +
                    "    * **ด้านการกระจายงานรายสัปดาห์:** รูปแบบการทำงานในแต่ละสัปดาห์เป็นอย่างไร มีสัปดาห์ไหนที่ทำงานหนักเป็นพิเศษหรือไม่ และการกระจายตัวของงานเหมาะสมหรือไม่\n\n" +
                    "3. **ข้อเสนอแนะเพื่อการพัฒนา (Actionable Recommendations):**\n" +
                    "    * ให้คำแนะนำที่นำไปปฏิบัติได้จริง 2-3 ข้อเพื่อปรับปรุงการทำงานในเดือนถัดไป เช่น การบริหารจัดการเวลา, การจัดลำดับความสำคัญของงานที่ยังไม่เสร็จ หรือการกระจายงานให้สมดุลมากขึ้น";

            // Call LLM Service to analyze data
            GenerateContentResponse response = vertexAIClient.models.generateContent("gemini-2.5-flash", prompt, null);
            System.out.println(response.text());
            return response.text();
        } catch (Exception e) {
            System.out.println("Error analyzing report data: " + e.getMessage() + e.getCause());
            return "ไม่สามารถวิเคราะห์ข้อมูลได้ในขณะนี้";
        }
    }

}
