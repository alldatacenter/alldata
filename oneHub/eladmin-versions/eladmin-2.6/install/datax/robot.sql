/*
 Navicat Premium Data Transfer

 Source Server         : 本地
 Source Server Type    : MySQL
 Source Server Version : 50730
 Source Host           : localhost:3306
 Source Schema         : robot

 Target Server Type    : MySQL
 Target Server Version : 50730
 File Encoding         : 65001

 Date: 03/05/2022 12:07:48
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for robot_patient
-- ----------------------------
DROP TABLE IF EXISTS `robot_patient`;
CREATE TABLE `robot_patient`  (
  `id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '主键',
  `patient_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '患者姓名',
  `patient_sex` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '患者性别（1男2女）',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '患者表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of robot_patient
-- ----------------------------
INSERT INTO `robot_patient` VALUES ('0479951f8964175e624b2ca61ee0e835', '侯玉宇', '2');
INSERT INTO `robot_patient` VALUES ('34ddebc05f7c9fb769fcd020028203d6', '庞日成', '1');
INSERT INTO `robot_patient` VALUES ('7b137ca2d563086b9e4fadee385b50b8', '', '2');

-- ----------------------------
-- Table structure for robot_symptom_part
-- ----------------------------
DROP TABLE IF EXISTS `robot_symptom_part`;
CREATE TABLE `robot_symptom_part`  (
  `id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '主键',
  `part_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '部位名称',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '部位表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of robot_symptom_part
-- ----------------------------
INSERT INTO `robot_symptom_part` VALUES ('3244c36870e4a47ef1fc6e2c1acf00a2', '眼耳口鼻');
INSERT INTO `robot_symptom_part` VALUES ('4c4c0e819fc95c09dde308cc04df6256', '双下肢');
INSERT INTO `robot_symptom_part` VALUES ('608dd4772103c7b3a198641cf842a4e1', '双上肢');
INSERT INTO `robot_symptom_part` VALUES ('62207ec3cd713e906c461dfbfddf6504', '肩部');
INSERT INTO `robot_symptom_part` VALUES ('6b866f5e2ee092c0d2d0ffd5d9fea78b', '腰部');
INSERT INTO `robot_symptom_part` VALUES ('88806113c934137edd261d1e8a4f1f72', '颈部');
INSERT INTO `robot_symptom_part` VALUES ('92edaaa5ee8d304d9f01950e7979d2ab', '头部');
INSERT INTO `robot_symptom_part` VALUES ('96f7f70bf165d11a1161e19e2917ad65', '其他');
INSERT INTO `robot_symptom_part` VALUES ('a43438901e6b5f56d8aff49ea0c423d6', '生殖器');
INSERT INTO `robot_symptom_part` VALUES ('aaecbc8d28a302e5b89740e3d4ccf3b8', '臀部');
INSERT INTO `robot_symptom_part` VALUES ('c752554d2a789e07774532201a0876d9', '背部');
INSERT INTO `robot_symptom_part` VALUES ('c826bc719672e94482422eb355bcdee6', '皮肤');
INSERT INTO `robot_symptom_part` VALUES ('d311f223dd97959e447bcc4e63e38c22', '胸部');
INSERT INTO `robot_symptom_part` VALUES ('e7d8dc2171ec4f64b895b82c5d627459', '排泄部');
INSERT INTO `robot_symptom_part` VALUES ('ea4766dfb3950d787b4fae051c525a13', '腹部');

-- ----------------------------
-- Table structure for robot_symptom_type
-- ----------------------------
DROP TABLE IF EXISTS `robot_symptom_type`;
CREATE TABLE `robot_symptom_type`  (
  `id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '主键',
  `part_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '所属部位',
  `type_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '症状名称',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '症状表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of robot_symptom_type
-- ----------------------------
INSERT INTO `robot_symptom_type` VALUES ('314292492987dbac45618f0633ad5fba', 'd311f223dd97959e447bcc4e63e38c22', '咳嗽');
INSERT INTO `robot_symptom_type` VALUES ('5e7924f1a27fc08a817dd31820fa3736', '62207ec3cd713e906c461dfbfddf6504', '肩关节活动受限');
INSERT INTO `robot_symptom_type` VALUES ('7e696f88dfec22d719b033608a21b387', '62207ec3cd713e906c461dfbfddf6504', '右肩背有放射痛');
INSERT INTO `robot_symptom_type` VALUES ('fdeee4116cd6c0a16fe15fe1bfc208ef', 'd311f223dd97959e447bcc4e63e38c22', '咳痰');

SET FOREIGN_KEY_CHECKS = 1;
