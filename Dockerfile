# ================= STAGE 1: Build =================
# ใช้ Gradle image ที่มี JDK 17 เพื่อทำการ build โปรเจกต์
FROM gradle:8.3.0-jdk17-jammy AS builder

# ตั้งค่า working directory
WORKDIR /app

# คัดลอกเฉพาะไฟล์ที่จำเป็นสำหรับการ download dependencies ก่อน
# เพื่อใช้ประโยชน์จาก Docker layer caching
COPY build.gradle settings.gradle ./
COPY gradle ./gradle

# Download dependencies
RUN gradle build --no-daemon || return 0

# คัดลอก source code ทั้งหมด
COPY src ./src

# Build โปรเจกต์ให้เป็น .jar file ที่สมบูรณ์
RUN gradle bootJar --no-daemon

# ================= STAGE 2: Runtime =================
# ใช้ Java 17 JRE (Java Runtime Environment) ที่มีขนาดเล็กสำหรับ run app
FROM eclipse-temurin:17-jre-jammy

# ตั้งค่า working directory
WORKDIR /app

# คัดลอก .jar file ที่ build เสร็จแล้วจาก Stage 1
COPY --from=builder /app/build/libs/*.jar app.jar

# กำหนด Port ที่ Application ของคุณจะรัน (ปกติ Spring Boot คือ 8080)
EXPOSE 8080

# คำสั่งสำหรับ run application เมื่อ container เริ่มทำงาน
ENTRYPOINT ["java", "-jar", "app.jar"]