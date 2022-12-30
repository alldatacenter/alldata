drop database if exists recommend;
create database if not exists recommend;
use recommend;
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;
-- ----------------------------
-- Table Structure for product
-- ----------------------------

DROP TABLE IF EXISTS `product`;
CREATE TABLE `product`  (
                            `productId` int(20) NOT NULL,
                            `name` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
                            `imageUrl` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
                            `categories` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
                            `tags` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
                            INDEX indexProductId (`productId`)
) ENGINE = InnoDB  CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table Structure for rating
-- ----------------------------
DROP TABLE IF EXISTS `rating`;
CREATE TABLE `rating`  (
                           `userId` int(20) NOT NULL,
                           `productId` int(20) NOT NULL,
                           `score` decimal(5, 1) NOT NULL,
                           `timestamp` int(20) NOT NULL,
                           INDEX indexUserId (`userId`)
) ENGINE = InnoDB  CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;


-- ----------------------------
-- Table Structure for user
-- ----------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user`  (
                         `id` int(11) NOT NULL AUTO_INCREMENT,
                         `name` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
                         `password` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
                         `timestamp` long NOT NULL,
                         PRIMARY KEY (`id`) USING BTREE,
                         INDEX indexId (`id`)
) ENGINE = InnoDB  CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;
SET FOREIGN_KEY_CHECKS = 1;

insert into product(productId, name, imageUrl, categories, tags) values (425715, "我们仨(大字版)", "https://images-cn-4.ssl-images-amazon.com/images/I/416uOM6O9nL._SY344_BO1,204,203,200_QL70_.jpg", "文学艺术|文学类|图书音像", "书|散文|文学|杨绛|钱钟书|好看|很感人");
insert into product(productId, name, imageUrl, categories, tags) values (127544, "可悠然美肌沐浴露550ml(恬静清香 )(进)", "https://images-cn-4.ssl-images-amazon.com/images/I/41VlhEnW8hL._SY300_QL70_.jpg", "沐浴露|身体护理|美妆个护", "沐浴露|可悠然|好用|用起来很舒服");
insert into product(productId, name, imageUrl, categories, tags) values (3982, "Fuhlen 富勒 M8眩光舞者时尚节能无线鼠标(草绿)(眩光.悦动.时尚炫舞鼠标 12个月免换电池 高精度光学寻迹引擎 超细微接收器10米传输距离)", "https://images-cn-4.ssl-images-amazon.com/images/I/31QPvUDNavL._SY300_QL70_.jpg", "外设产品|鼠标|电脑/办公", "富勒|鼠标|电子产品|好用|外观漂亮");
insert into product(productId, name, imageUrl, categories, tags) values (259637, "小狗钱钱", "https://images-cn-4.ssl-images-amazon.com/images/I/51oNLo7MsmL._SY344_BO1,204,203,200_QL70_.jpg", "图书音像|少儿|少儿/教育图书", "书|少儿图书|教育类|童书|不错|孩子很喜欢");
insert into product(productId, name, imageUrl, categories, tags) values (437230, "三杯茶", "https://images-cn-4.ssl-images-amazon.com/images/I/5146UDza%2BDL._SY344_BO1,204,203,200_QL70_.jpg", "小说|文学艺术|图书音像", "书|散文|文学|写的不错|内容不错");
insert into product(productId, name, imageUrl, categories, tags) values (260348, "西尔斯亲密育儿百科(全球最权威最受欢迎的育儿百科最新定本)", "https://images-cn-4.ssl-images-amazon.com/images/I/41XLbU3%2B3lL._SY344_BO1,204,203,200_QL70_.jpg", "育儿/早教|生活类图书|图书音像", "书|育儿类|教育类|不错|内容丰富|西尔斯");
insert into product(productId, name, imageUrl, categories, tags) values (6797, "PHILIPS飞利浦HQ912/15两刀头充电式电动剃须刀", "https://images-cn-4.ssl-images-amazon.com/images/I/415UjOLnBML._SY300_QL70_.jpg", "家用电器|个人护理电器|电动剃须刀", "飞利浦|剃须刀|家用电器|好用|外观漂亮");
insert into product(productId, name, imageUrl, categories, tags) values (138554, "这些都是你给我的爱", "https://images-cn-4.ssl-images-amazon.com/images/I/41yPN7iuAoL._SY344_BO1,204,203,200_QL70_.jpg", "图书音像|少儿|少儿/教育图书", "书|少儿图书|教育类|励志|鸡汤|好看|内容不错");
insert into product(productId, name, imageUrl, categories, tags) values (8195, "Kingston 金士顿 Class4 32G TF卡(micro SD)手机存储卡", "https://images-cn-4.ssl-images-amazon.com/images/I/41zUS3RjvRL._SY300_QL70_.jpg", "存储设备|存储卡|电脑/办公", "存储卡|金士顿|SD卡|容量挺大的|速度快|好用");
insert into product(productId, name, imageUrl, categories, tags) values (275707, "猜猜我有多爱你", "https://images-cn-4.ssl-images-amazon.com/images/I/51OaoKgR8RL._SX258_BO1,204,203,200_QL70_.jpg", "图书音像|少儿|少儿/教育图书", "书|教育类|不错|内容丰富|少儿图书|好看");
insert into product(productId, name, imageUrl, categories, tags) values (438242, "EARISE 雅兰仕AL-202 2.0声道 线控 笔记本音箱 (黑色)", "https://images-cn-4.ssl-images-amazon.com/images/I/41F1x3mdPsL._SY300_QL70_.jpg", "外设产品|电脑/办公|电脑音箱", "雅兰仕|音箱|电子产品|外观漂亮");
insert into product(productId, name, imageUrl, categories, tags) values (286997, "千纤草黄瓜水500ml", "https://images-cn-4.ssl-images-amazon.com/images/I/31i7lIchHBL._SY300_QL70_.jpg", "面部护理|美妆个护|化妆水/爽肤水", "化妆品|面部护理|千纤草|到货速度快|用起来很舒服");
insert into product(productId, name, imageUrl, categories, tags) values (13316, "Kingston 金士顿 DataTraveler 101 G2 32GB 优盘", "https://images-cn-4.ssl-images-amazon.com/images/I/41nIbzEKQCL._SY300_QL70_.jpg", "存储设备|U盘|电脑/办公", "优盘|金士顿|好用|容量挺大的|速度快");
insert into product(productId, name, imageUrl, categories, tags) values (139060, "Neutrogena露得清深层净化洗面乳二支特惠装100g*2(特卖)(新老包装更替中,随机发货)", "https://images-cn-4.ssl-images-amazon.com/images/I/41QbMo3uK%2BL._SY300_QL70_.jpg", "洁面|面部护理|美妆个护", "洗面奶|化妆品|露得清|好用|用起来很舒服|到货速度快");
insert into product(productId, name, imageUrl, categories, tags) values (13543, "蔡康永的说话之道", "https://images-cn-4.ssl-images-amazon.com/images/I/51RCZJf9vSL._SY344_BO1,204,203,200_QL70_.jpg", "青春文学|文学艺术|图书音像", "蔡康永|写的真好|不贵|内容不错|书");
insert into product(productId, name, imageUrl, categories, tags) values (294209, "不畏将来 不念过去", "https://images-cn-4.ssl-images-amazon.com/images/I/51DbBJiAbOL._SY344_BO1,204,203,200_QL70_.jpg", "政治/军事|人文社科类图书|图书音像", "书|军事类|政治类|好看|有破损|内容不错");
insert into product(productId, name, imageUrl, categories, tags) values (442300, "人生若只如初见", "https://images-cn-4.ssl-images-amazon.com/images/I/41kcaeWon7L._SY344_BO1,204,203,200_QL70_.jpg", "文学艺术|文学类|图书音像", "书|文学|诗词|写的不错|好看|通俗易懂|到货速度快");
insert into product(productId, name, imageUrl, categories, tags) values (300265, "Edifier漫步者 H180 耳塞式耳机 白色(经典时尚)", "https://images-cn-4.ssl-images-amazon.com/images/I/41LUEX%2BDciL._SY300_QL70_.jpg", "外设产品|耳机/耳麦|电脑/办公", "耳机|耳塞式耳机|电子产品|漫步者|外观漂亮|质量好");
insert into product(productId, name, imageUrl, categories, tags) values (14103, "SanDisk 闪迪 microSDXC Class10 64GB至尊高速移动存储卡 UHS-1制式 读写速度最高可达30MB/s", "https://images-cn-4.ssl-images-amazon.com/images/I/41mU1iHKkkL._SX300_QL70_.jpg", "存储设备|存储卡|电脑/办公", "存储卡|SD卡|容量挺大的|闪迪|速度快|好用");
insert into product(productId, name, imageUrl, categories, tags) values (145218, "好妈妈胜过好老师", "https://images-cn-4.ssl-images-amazon.com/images/I/41DBneaGRwL._SY344_BO1,204,203,200_QL70_.jpg", "亲子/家教|生活类图书|图书音像", "书|教育类|写的很好|尹建莉老师的书真好|很有启发|到货速度快|有破损");
insert into product(productId, name, imageUrl, categories, tags) values (21643, "L'OREAL PARIS巴黎欧莱雅男士劲能醒肤露 8重功效50ml", "https://images-cn-4.ssl-images-amazon.com/images/I/41%2B4D64-AEL._SY300_QL70_.jpg", "男士乳液/面霜|男士护肤|美妆个护", "好用|护肤品|到货速度快|欧莱雅");
insert into product(productId, name, imageUrl, categories, tags) values (302217, "Elizabeth Arden伊丽莎白雅顿绿茶香水50ml(进)(特卖)", "https://images-cn-4.ssl-images-amazon.com/images/I/41AAc8vsA5L._SY300_QL70_.jpg", "彩妆|香水|美妆个护", "化妆品|伊丽莎白|香水|好用|很香|到货速度快");
insert into product(productId, name, imageUrl, categories, tags) values (442353, "三国演义", "https://images-cn-4.ssl-images-amazon.com/images/I/51Wyy6utXlL._SY344_BO1,204,203,200_QL70_.jpg", "小说|文学艺术|图书音像", "书|文学|四大名著|罗贯中|好看|值得一读再读|传统文化|小说");
insert into product(productId, name, imageUrl, categories, tags) values (314081, "时寒冰说:经济大棋局,我们怎么办", "https://images-cn-4.ssl-images-amazon.com/images/I/51DDJuy6zbL._SX258_BO1,204,203,200_QL70_.jpg", "政治/军事|人文社科类图书|图书音像", "书|军事类|政治类|时寒冰|经管类|扯淡|很有启发|好看");
insert into product(productId, name, imageUrl, categories, tags) values (154318, "Cetaphil丝塔芙洗面奶200ml(特卖)", "https://images-cn-4.ssl-images-amazon.com/images/I/41alSseXdXL._SY300_QL70_.jpg", "洁面|面部护理|美妆个护", "化妆品|洗面奶|面部护理|用起来很舒服|丝塔芙");
insert into product(productId, name, imageUrl, categories, tags) values (323519, "Rapoo 雷柏 1090光学鼠标(经典黑)(智能自动对码/1000DPI高精度光学引擎)", "https://images-cn-4.ssl-images-amazon.com/images/I/410NDN9q1EL._SY300_QL70_.jpg", "外设产品|鼠标|电脑/办公", "电子产品|鼠标|外设|质量好|雷柏|到货速度快|外观漂亮|好用");
insert into product(productId, name, imageUrl, categories, tags) values (443787, "美的加湿器S20U-M米宝宝(颜色随机)", "https://images-cn-4.ssl-images-amazon.com/images/I/415XuLOUXOL._SY300_QL70_.jpg", "家用电器|加湿/除湿器|生活电器", "美的|家用电器|加湿器|外观漂亮|质量好|好用");
insert into product(productId, name, imageUrl, categories, tags) values (326582, "Mentholatum曼秀雷敦男士冰爽活炭洁面乳150ml (特卖)", "https://images-cn-4.ssl-images-amazon.com/images/I/41Jc24lBavL._SY300_QL70_.jpg", "男士护肤|美妆个护|男士洁面", "化妆品|男士|曼秀雷敦|好用|用起来很舒服|到货速度快");
insert into product(productId, name, imageUrl, categories, tags) values (155001, "PHILIPS飞利浦HP8203/00均衡呵护系列负离子功能电吹风", "https://images-cn-4.ssl-images-amazon.com/images/I/41vXzDEzLjL._SY300_QL70_.jpg", "家用电器|电吹风|个人护理电器", "飞利浦|电吹风|外观漂亮|质量好|家用电器");
insert into product(productId, name, imageUrl, categories, tags) values (333125, "素年锦时", "https://images-cn-4.ssl-images-amazon.com/images/I/41Pzfrt4ZVL._SY344_BO1,204,203,200_QL70_.jpg", "音像|图书音像|有声读物", "书|有声读物|青春文学|文学|小说|好看|狗血");
insert into product(productId, name, imageUrl, categories, tags) values (457976, "我们仨", "https://images-cn-4.ssl-images-amazon.com/images/I/41d6KLz9MDL._SY344_BO1,204,203,200_QL70_.jpg", "传记|人文社科类图书|图书音像", "书|散文|文学|杨绛|钱钟书|好看|很感人");
insert into product(productId, name, imageUrl, categories, tags) values (352021, "Lenovo 联想 A820T TD-SCDMA/GSM 双卡双待 3G手机(白色 移动定制) 四核1.2G处理器 800万像素", "https://images-cn-4.ssl-images-amazon.com/images/I/41GOej5rPUL._SX300_QL70_.jpg", "手机|手机通讯|手机/数码", "联想|手机|质量好|联想手机还不错|好用|待机时间长");
insert into product(productId, name, imageUrl, categories, tags) values (156103, "气场修习术", "https://images-cn-4.ssl-images-amazon.com/images/I/51tk-seyYsL._SY344_BO1,204,203,200_QL70_.jpg", "经管类图书|成功/励志|图书音像", "书|经管类|励志|鸡汤|垃圾");
insert into product(productId, name, imageUrl, categories, tags) values (353799, "沉思录", "https://images-cn-4.ssl-images-amazon.com/images/I/41MA9JZAjPL._SY344_BO1,204,203,200_QL70_.jpg", "哲学/宗教|人文社科类图书|图书音像", "书|哲学类|励志|值得一读再读|总理推荐|很有启发");
insert into product(productId, name, imageUrl, categories, tags) values (459600, "儿童动物百科全书", "https://images-cn-4.ssl-images-amazon.com/images/I/61fOjNlt39L._SX258_BO1,204,203,200_QL70_.jpg", "育儿/早教|生活类图书|图书音像", "书|早教|教育类|育儿类|少儿图书|百科全书|内容丰富");
insert into product(productId, name, imageUrl, categories, tags) values (365357, "怀孕40周完美方案(升级畅销版)", "https://images-cn-4.ssl-images-amazon.com/images/I/51NKwb2S-jL._SX258_BO1,204,203,200_QL70_.jpg", "孕产/胎教|生活类图书|图书音像", "书|育儿类|教育类|内容丰富|有破损|胎教类|到货速度快|孕妇必读");
insert into product(productId, name, imageUrl, categories, tags) values (160597, "白鹿原", "https://images-cn-4.ssl-images-amazon.com/images/I/41BXjDoU9HL._SY344_BO1,204,203,200_QL70_.jpg", "小说|文学艺术|图书音像", "书|小说|文学|好看|内容不错|中国现代最好的小说|对那个时代描写很好|陈忠实的代表作");
insert into product(productId, name, imageUrl, categories, tags) values (367460, "Lenovo 联想 A820T 3G手机(深邃黑)TD-SCDMA/GSM 双卡双待 四核1.2G处理器 800万像素", "https://images-cn-4.ssl-images-amazon.com/images/I/412LjS0KhpL._SY300_QL70_.jpg", "手机|手机通讯|手机/数码", "手机|联想|待机时间长|好用|质量好");
insert into product(productId, name, imageUrl, categories, tags) values (460360, "Styliving宝裕合BFNC066B烤黑漆迷你四层置物架", "https://images-cn-4.ssl-images-amazon.com/images/I/41Ri4BCfTeL._SY300_QL70_.jpg", "收纳整理|收纳|家居生活", "家居生活|好用|质量好|到货速度快");
insert into product(productId, name, imageUrl, categories, tags) values (371783, "现代汉语词典(第6版)", "https://images-cn-4.ssl-images-amazon.com/images/I/511Gu6tkFuL._SY344_BO1,204,203,200_QL70_.jpg", "其它图书|工具书|图书音像", "书|词典|好用|质量好");
insert into product(productId, name, imageUrl, categories, tags) values (167781, "Motorola 摩托罗拉 XT788 3G手机(黑色 电信定制)CDMA2000/GSM 双模双待 1.2GHz双核 Android 4.0系统 4.3寸康宁防刮玻璃屏 800M背照式摄像头 9.9mm超薄机身", "https://images-cn-4.ssl-images-amazon.com/images/I/51Cs2hDkupL._SY300_QL70_.jpg", "手机|手机通讯|手机/数码", "摩托罗拉|手机|到货速度快|好用|电子产品");
insert into product(productId, name, imageUrl, categories, tags) values (379243, "苏菲的世界", "https://images-cn-4.ssl-images-amazon.com/images/I/41yL3NnPJgL._SY344_BO1,204,203,200_QL70_.jpg", "图书音像|少儿/教育图书|考试", "书|哲学类|通俗易懂|值得一读再读");
insert into product(productId, name, imageUrl, categories, tags) values (469516, "傲慢与偏见(英文版)", "https://images-cn-4.ssl-images-amazon.com/images/I/51oDRwjsCaL._SX258_BO1,204,203,200_QL70_.jpg", "图书音像|外语|少儿/教育图书", "书|小说|文学|英语|简奥斯汀|英文小说|好看");
insert into product(productId, name, imageUrl, categories, tags) values (383502, "不抱怨的世界(增订版)(附不抱怨手环1只)", "https://images-cn-4.ssl-images-amazon.com/images/I/51tIPRToL3L._SX258_BO1,204,203,200_QL70_.jpg", "手工/DIY|生活类图书|图书音像", "书|鸡汤|励志|很有启发|呵呵");
insert into product(productId, name, imageUrl, categories, tags) values (183418, "谁的青春不迷茫", "https://images-cn-4.ssl-images-amazon.com/images/I/41TEclaV%2BfL._SY344_BO1,204,203,200_QL70_.jpg", "图书音像|少儿|少儿/教育图书", "书|少儿图书|鸡汤|呵呵|励志|垃圾");
insert into product(productId, name, imageUrl, categories, tags) values (387693, "我的第一本专注力训练书", "https://images-cn-4.ssl-images-amazon.com/images/I/61%2BcsqqaWtL._SX258_BO1,204,203,200_QL70_.jpg", "图书音像|少儿|少儿/教育图书", "书|少儿图书|教育类|内容丰富|很有启发");
insert into product(productId, name, imageUrl, categories, tags) values (470839, "韩寒:独唱团(第1辑)", "https://images-cn-4.ssl-images-amazon.com/images/I/51wDiU3JacL._SY344_BO1,204,203,200_QL70_.jpg", "青春文学|文学艺术|图书音像", "书|杂志|韩寒|文学|独唱团|好看|只出了一期|呵呵");
insert into product(productId, name, imageUrl, categories, tags) values (406697, "Merries花王纸尿裤S82片(日本进口全新款)适合4-8kg", "https://images-cn-4.ssl-images-amazon.com/images/I/51GdcVwBEeL._SY300_QL70_.jpg", "尿裤湿巾|纸尿裤/拉拉裤/纸尿片|母婴/玩具", "纸尿裤|好用|花王|母婴用品|到货速度快|质量好");
insert into product(productId, name, imageUrl, categories, tags) values (183679, "全新Kindle Paperwhite电子书阅读器", "https://images-cn-4.ssl-images-amazon.com/images/I/41xDoQ9vvTL._SY300_QL70_.jpg", "数码影音|电纸书/电子阅览器|手机/数码", "电子产品|亚马逊|电纸书|kindle|好用|到货速度快|质量好");
insert into product(productId, name, imageUrl, categories, tags) values (407523, "送你一颗子弹", "https://images-cn-4.ssl-images-amazon.com/images/I/41hIu%2BPUP%2BL._SY344_BO1,204,203,200_QL70_.jpg", "卡通|音像|图书音像", "书|到货速度快|散文|刘瑜的书必须支持|有破损");
insert into product(productId, name, imageUrl, categories, tags) values (473113, "活着", "https://images-cn-4.ssl-images-amazon.com/images/I/51UETWQschL._SY344_BO1,204,203,200_QL70_.jpg", "音像|图书音像|有声读物", "书|小说|余华|很感人|文学|看的很压抑|写的很好");
insert into product(productId, name, imageUrl, categories, tags) values (407816, "\"幸福了吗?(附完整版""白岩松耶鲁大学演讲""光盘1张)\"", "https://images-cn-4.ssl-images-amazon.com/images/I/51PuWq263LL._SY344_BO1,204,203,200_QL70_.jpg", "图书音像|外语|少儿/教育图书", "书|教育类|鸡汤|励志|白岩松|演讲");
insert into product(productId, name, imageUrl, categories, tags) values (184282, "追风筝的人", "https://images-cn-4.ssl-images-amazon.com/images/I/41Zu6wuZ4VL._SY344_BO1,204,203,200_QL70_.jpg", "小说|文学艺术|图书音像", "书|小说|文学|很感动|哭的稀里哗啦|好看|到货速度快|内容挺好|内容不错");
insert into product(productId, name, imageUrl, categories, tags) values (410608, "哈尔斯HB-500F真空吊带保温杯500ml", "https://images-cn-4.ssl-images-amazon.com/images/I/41QCHGhjlML._SY300_QL70_.jpg", "保温杯/保温壶|厨房/餐具|家居生活", "保温杯|哈尔斯|餐具|好用|质量好");
insert into product(productId, name, imageUrl, categories, tags) values (474201, "如何说孩子才会听 怎么听孩子才肯说", "https://images-cn-4.ssl-images-amazon.com/images/I/51Qk-5K4BTL._SY344_BO1,204,203,200_QL70_.jpg", "亲子/家教|生活类图书|图书音像", "书|教育类|育儿类|内容丰富|很有启发");
insert into product(productId, name, imageUrl, categories, tags) values (410716, "中日交流标准日本语(初级)(套装上下册)", "https://images-cn-4.ssl-images-amazon.com/images/I/51PtQsbW%2B-L._SX258_BO1,204,203,200_QL70_.jpg", "图书音像|外语|少儿/教育图书", "书|教育类|日语|教材|到货速度快");
insert into product(productId, name, imageUrl, categories, tags) values (186429, "麦田里的守望者", "https://images-cn-4.ssl-images-amazon.com/images/I/41AqJNk7d7L._SY344_BO1,204,203,200_QL70_.jpg", "小说|文学艺术|图书音像", "书|很感动|塞林格|有点颓废|小说|好看|内容不错|文学");
insert into product(productId, name, imageUrl, categories, tags) values (474853, "查名一猫防水防泪睫毛组合(睫毛膏8ml+眼线液6ml)", "https://images-cn-4.ssl-images-amazon.com/images/I/31fB78eT38L._SY300_QL70_.jpg", "睫毛膏|彩妆|美妆个护", "化妆品|睫毛膏|好用|质量好|用起来很舒服");
insert into product(productId, name, imageUrl, categories, tags) values (201454, "小王子(65周年纪念版)", "https://images-cn-4.ssl-images-amazon.com/images/I/51KgEWM2BiL._SY344_BO1,204,203,200_QL70_.jpg", "图书音像|少儿|少儿/教育图书", "书|小说|文学|很感动|哭的稀里哗啦|看了很多遍|法国");
insert into product(productId, name, imageUrl, categories, tags) values (487126, "虚实之间", "https://images-cn-4.ssl-images-amazon.com/images/I/412qSligE2L._SY344_BO1,204,203,200_QL70_.jpg", "文学艺术|文学类|图书音像", "书|文学|小说|好看|内容不错|到货速度快");
insert into product(productId, name, imageUrl, categories, tags) values (203971, "如何说孩子才会听 怎么听孩子才肯说(2012修订版)", "https://images-cn-4.ssl-images-amazon.com/images/I/516txkpnK%2BL._SY344_BO1,204,203,200_QL70_.jpg", "亲子/家教|生活类图书|图书音像", "书|教育类|少儿图书|到货速度快|有破损|很有启发");
insert into product(productId, name, imageUrl, categories, tags) values (489369, "英语词汇的奥秘(修订本)", "https://images-cn-4.ssl-images-amazon.com/images/I/51dPPX1EIdL._SY344_BO1,204,203,200_QL70_.jpg", "图书音像|少儿/教育图书|考试", "书|词典|英语|教育类|写的不错");
insert into product(productId, name, imageUrl, categories, tags) values (204025, "围城", "https://images-cn-4.ssl-images-amazon.com/images/I/51G8wGPoHgL._SY344_BO1,204,203,200_QL70_.jpg", "小说|文学艺术|图书音像", "书|小说|文学|钱钟书|讽刺很犀利|好看|有破损");
insert into product(productId, name, imageUrl, categories, tags) values (492579, "红楼梦", "https://images-cn-4.ssl-images-amazon.com/images/I/51ebqcYP-gL._SY344_BO1,204,203,200_QL70_.jpg", "图书音像|少儿|少儿/教育图书", "书|小说|文学|四大名著|值得一读再读|曹雪芹|经典|传统文化");
insert into product(productId, name, imageUrl, categories, tags) values (206404, "0-3岁小婴孩必备全书:经典童话本(套装10本)(童话大师传世名作)", "https://images-cn-4.ssl-images-amazon.com/images/I/51Aj56mwviL._SX258_BO1,204,203,200_QL70_.jpg", "图书音像|少儿|少儿/教育图书", "书|教育类|童话|好看|内容丰富|孩子看了很喜欢");
insert into product(productId, name, imageUrl, categories, tags) values (499898, "人间词话 手稿本全编", "https://images-cn-4.ssl-images-amazon.com/images/I/51tZT18-GsL._SY344_BO1,204,203,200_QL70_.jpg", "文学艺术|文学类|图书音像", "书|经典|传统文化|王国维|文学|没得说");
insert into product(productId, name, imageUrl, categories, tags) values (218439, "何以笙箫默(7周年精装珍藏版)", "https://images-cn-4.ssl-images-amazon.com/images/I/51DbRsaJQ8L._SX258_BO1,204,203,200_QL70_.jpg", "哲学/宗教|人文社科类图书|图书音像", "书|小说|好看|垃圾|呵呵|狗血爱情");
insert into product(productId, name, imageUrl, categories, tags) values (501559, "A4Tech 双飞燕OP-520NU有线针光鼠标USB接口 黑色 (针光引擎:新一代针光技术,超强过界 穿透三维,定位精准,毛上也照飞)", "https://images-cn-4.ssl-images-amazon.com/images/I/31k9g-C2h-L._SY300_QL70_.jpg", "外设产品|鼠标|电脑/办公", "鼠标|双飞燕|办公用品|外设|质量好");
insert into product(productId, name, imageUrl, categories, tags) values (228884, "撒哈拉的故事", "https://images-cn-4.ssl-images-amazon.com/images/I/41liBBduGwL._SY344_BO1,204,203,200_QL70_.jpg", "图书音像|少儿|少儿/教育图书", "书|三毛|散文|好看|励志|内容不错|还不错");
insert into product(productId, name, imageUrl, categories, tags) values (505556, "Casio 卡西欧EDIFICE系列三眼六针石英男表 EF-305D-1AVDR", "https://images-cn-4.ssl-images-amazon.com/images/I/51hdpobqULL._SY300_QL70_.jpg", "钟表/首饰/眼镜/礼品|钟表|手表", "卡西欧|手表|男士|质量好|不贵");
insert into product(productId, name, imageUrl, categories, tags) values (231449, "\"致我们终将逝去的青春(附""致青春""珍藏卡册)\"", "https://images-cn-4.ssl-images-amazon.com/images/I/51EzZjyJYfL._SX258_BO1,204,203,200_QL70_.jpg", "青春文学|文学艺术|图书音像", "书|狗血|看了电影买的书|有破损|到货速度快|小说|青春文学|文学");
insert into product(productId, name, imageUrl, categories, tags) values (507644, "你是人间的四月天", "https://images-cn-4.ssl-images-amazon.com/images/I/41n9yJRcBTL._SY344_BO1,204,203,200_QL70_.jpg", "小说|文学艺术|图书音像", "书|文学|诗词|林徽因|写的不错|真好");
insert into product(productId, name, imageUrl, categories, tags) values (250451, "最好的时光在路上:中国国家地理", "https://images-cn-4.ssl-images-amazon.com/images/I/51avrbeLOKL._SY344_BO1,204,203,200_QL70_.jpg", "旅游/地图|生活类图书|图书音像", "书|旅行|国家地理系列|好看|内容丰富");
insert into product(productId, name, imageUrl, categories, tags) values (508447, "小时代3.0刺金时代(限量版)(附郭敬明最新写真明信片+海报二十张+特制3D眼镜+精美大开海报4张)", "https://images-cn-4.ssl-images-amazon.com/images/I/514%2ByTYTgOL._SY344_BO1,204,203,200_QL70_.jpg", "青春文学|文学艺术|图书音像", "青春文学|书|小说|呵呵|郭敬明|垃圾|拜金|狗血");
insert into product(productId, name, imageUrl, categories, tags) values (253869, "牛津高阶英汉双解词典(第7版)", "https://images-cn-4.ssl-images-amazon.com/images/I/51l0VjtzefL._SY344_BO1,204,203,200_QL70_.jpg", "图书音像|外语|少儿/教育图书", "书|英语|词典|好用|不愧是牛津大学出的|经典|到货速度快");
insert into product(productId, name, imageUrl, categories, tags) values (511015, "万历十五年", "https://images-cn-4.ssl-images-amazon.com/images/I/51aFUdyCbXL._SY344_BO1,204,203,200_QL70_.jpg", "政治/军事|人文社科类图书|图书音像", "书|历史类|黄仁宇|政治类|军事类|值得一读再读|大历史观代表作");
insert into product(productId, name, imageUrl, categories, tags) values (258451, "Bear小熊 酸奶机 SNJ-10A (加送4个分杯内胆,绿色,亚马逊订制)", "https://images-cn-4.ssl-images-amazon.com/images/I/41YFb2l1IlL._SY300_QL70_.jpg", "家用电器|厨房电器|酸奶机", "小熊|家用电器|好用|酸奶机|到货速度快|质量好");
insert into product(productId, name, imageUrl, categories, tags) values (517740, "步步惊心(新版)(附精美明信片)", "https://images-cn-4.ssl-images-amazon.com/images/I/51cl56NMqNL._SY344_BO1,204,203,200_QL70_.jpg", "青春文学|文学艺术|图书音像", "书|青春文学|步步惊心|垃圾|狗血|呵呵|小说");
insert into product(productId, name, imageUrl, categories, tags) values (520943, "穆斯林的葬礼", "https://images-cn-4.ssl-images-amazon.com/images/I/417dsC9NdOL._SY344_BO1,204,203,200_QL70_.jpg", "工艺饰品|家具/家装/建材", "小说|书|好看|文学|狗血|很感人");
