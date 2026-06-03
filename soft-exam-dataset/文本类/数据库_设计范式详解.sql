-- ============================================================
-- 软件设计师考试 - 数据库设计范式详解与示例
-- 本SQL脚本演示了从1NF到BCNF的范式演变过程
-- 关联资料：详见《数据库系统_知识点总结.pdf》
-- ============================================================

-- 【第0步】创建数据库
CREATE DATABASE IF NOT EXISTS soft_exam_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE soft_exam_db;

-- ============================================================
-- 第一范式（1NF）：属性不可再分
-- 违反1NF的示例：联系方式字段包含电话和邮箱
-- ============================================================
CREATE TABLE student_0nf (
    student_id      INT PRIMARY KEY,
    student_name    VARCHAR(50) NOT NULL,
    contact_info    VARCHAR(200),      -- 违反1NF：包含多种联系方式
    courses         VARCHAR(500)       -- 违反1NF：包含多门课程
) COMMENT '违反1NF的示例表';

-- 满足1NF的修正：每个字段不可再分
CREATE TABLE student_1nf (
    student_id      INT PRIMARY KEY,
    student_name    VARCHAR(50) NOT NULL,
    phone           VARCHAR(20),
    email           VARCHAR(100)
) COMMENT '满足1NF：每个字段都是原子的';

-- 课程关联需要单独的表
CREATE TABLE student_course_1nf (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    student_id      INT NOT NULL,
    course_name     VARCHAR(100) NOT NULL,
    score           DECIMAL(5,2),
    FOREIGN KEY (student_id) REFERENCES student_1nf(student_id)
) COMMENT '1NF下的学生选课表';

-- ============================================================
-- 第二范式（2NF）：在1NF基础上，消除非主属性对码的部分函数依赖
-- 条件：关系模式R在1NF上，且每个非主属性完全函数依赖于码
-- 仅适用于联合主键的情况
-- ============================================================

-- 违反2NF的示例：选课表（联合主键 student_id + course_id）
CREATE TABLE enrollment_1nf (
    student_id      INT,             -- 联合主键的一部分
    course_id       INT,             -- 联合主键的一部分
    student_name    VARCHAR(50),     -- 部分依赖student_id
    course_name     VARCHAR(100),    -- 部分依赖course_id
    score           DECIMAL(5,2),    -- 完全依赖联合主键
    PRIMARY KEY (student_id, course_id)
) COMMENT '满足1NF但违反2NF：student_name部分依赖于student_id';

-- 满足2NF的修正：拆分为三张表
CREATE TABLE student_2nf (
    student_id      INT PRIMARY KEY,
    student_name    VARCHAR(50) NOT NULL,
    dept_name       VARCHAR(100)
) COMMENT '学生表：主键为student_id';

CREATE TABLE course_2nf (
    course_id       INT PRIMARY KEY,
    course_name     VARCHAR(100) NOT NULL,
    credit          DECIMAL(3,1),
    teacher_name    VARCHAR(50)
) COMMENT '课程表：主键为course_id';

CREATE TABLE enrollment_2nf (
    student_id      INT,
    course_id       INT,
    score           DECIMAL(5,2),
    PRIMARY KEY (student_id, course_id),
    FOREIGN KEY (student_id) REFERENCES student_2nf(student_id),
    FOREIGN KEY (course_id) REFERENCES course_2nf(course_id)
) COMMENT '选课表：score完全依赖于联合主键';

-- ============================================================
-- 第三范式（3NF）：在2NF基础上，消除非主属性对码的传递函数依赖
-- 条件：在R∈2NF，若每个非主属性都不传递依赖于码
-- 即：不存在 X→Y→Z（Y不是候选码，Z是非主属性）
-- ============================================================

-- 违反3NF的示例：student_2nf 中 dept_name 传递依赖于 student_id
-- student_id → dept_name → dept_phone（假设有系电话字段）
CREATE TABLE student_violate_3nf (
    student_id      INT PRIMARY KEY,
    student_name    VARCHAR(50) NOT NULL,
    dept_id         INT NOT NULL,
    dept_name       VARCHAR(100),    -- 传递依赖：student_id→dept_id→dept_name
    dept_phone      VARCHAR(20)      -- 传递依赖：student_id→dept_id→dept_phone
) COMMENT '违反3NF：dept_name和dept_phone传递依赖于student_id';

-- 满足3NF的修正：拆分出系别表
CREATE TABLE department_3nf (
    dept_id         INT PRIMARY KEY,
    dept_name       VARCHAR(100) NOT NULL,
    dept_phone      VARCHAR(20),
    dept_head       VARCHAR(50)
) COMMENT '系别表';

CREATE TABLE student_3nf (
    student_id      INT PRIMARY KEY,
    student_name    VARCHAR(50) NOT NULL,
    dept_id         INT NOT NULL,
    FOREIGN KEY (dept_id) REFERENCES department_3nf(dept_id)
) COMMENT '学生表：消除了传递依赖';

-- ============================================================
-- BC范式（BCNF）：在3NF基础上，消除主属性对码的部分/传递依赖
-- 条件：所有非平凡的函数依赖X→Y中，X都包含码
-- 也称为"修正的第三范式"或"3.5NF"
-- ============================================================

-- BCNF示例：仓库管理（候选码：(项目, 供应商) 和 (项目, 零件)）
-- 违反BCNF的情况较复杂，在考试中主要考察判断

-- ============================================================
-- 综合示例：图书管理系统数据库设计
-- 展示从ER图到关系模式的转换
-- ============================================================

-- 图书表
CREATE TABLE book (
    book_id         INT PRIMARY KEY AUTO_INCREMENT,
    isbn            VARCHAR(20) UNIQUE NOT NULL,
    title           VARCHAR(200) NOT NULL,
    author          VARCHAR(100),
    publisher       VARCHAR(100),
    publish_date    DATE,
    price           DECIMAL(10,2),
    category_id     INT,
    stock_quantity  INT DEFAULT 0,
    FOREIGN KEY (category_id) REFERENCES book_category(category_id)
) COMMENT '图书信息表';

-- 图书分类表
CREATE TABLE book_category (
    category_id     INT PRIMARY KEY AUTO_INCREMENT,
    category_name   VARCHAR(50) NOT NULL,
    parent_id       INT,
    FOREIGN KEY (parent_id) REFERENCES book_category(category_id)
) COMMENT '图书分类表（支持层级）';

-- 借阅记录表
CREATE TABLE borrow_record (
    record_id       INT PRIMARY KEY AUTO_INCREMENT,
    reader_id       INT NOT NULL,
    book_id         INT NOT NULL,
    borrow_date     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    due_date        DATETIME NOT NULL,
    return_date     DATETIME,
    status          ENUM('借阅中', '已归还', '已逾期') DEFAULT '借阅中',
    FOREIGN KEY (reader_id) REFERENCES reader(reader_id),
    FOREIGN KEY (book_id) REFERENCES book(book_id)
) COMMENT '借阅记录表';

-- ============================================================
-- 考试高频SQL语句
-- ============================================================

-- 1. 查询每个系的学生人数
-- SELECT dept_id, COUNT(*) AS student_count
-- FROM student_3nf
-- GROUP BY dept_id;

-- 2. 查询平均成绩大于80的学生
-- SELECT s.student_name, AVG(e.score) AS avg_score
-- FROM student_2nf s
-- JOIN enrollment_2nf e ON s.student_id = e.student_id
-- GROUP BY s.student_id, s.student_name
-- HAVING AVG(e.score) > 80;

-- 3. 查询选修了所有课程的学生（除法运算）
-- SELECT student_name FROM student_2nf s
-- WHERE NOT EXISTS (
--     SELECT * FROM course_2nf c
--     WHERE NOT EXISTS (
--         SELECT * FROM enrollment_2nf e
--         WHERE e.student_id = s.student_id AND e.course_id = c.course_id
--     )
-- );

-- ============================================================
-- 考试重点总结
-- 1. 范式判断：给出关系模式，判断满足第几范式
-- 2. ER图到关系模式的转换（1:1, 1:N, M:N）
-- 3. SQL查询：GROUP BY + HAVING, 子查询, JOIN
-- 4. 函数依赖与码的求解
-- 5. 反范式化：适当牺牲范式以提高查询性能
-- 关联资料：详见《数据库_设计范式详解.sql》和《题库_数据结构与算法.json》
-- ============================================================
