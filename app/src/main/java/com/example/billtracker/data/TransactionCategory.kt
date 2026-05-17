package com.example.billtracker.data

enum class TransactionCategory(val displayName: String) {
    FOOD("餐饮"),
    TRANSPORT("交通"),
    SHOPPING("购物"),
    BILLS("生活缴费"),
    ENTERTAINMENT("娱乐"),
    MEDICAL("医疗"),
    TRANSFER("转账"),
    REFUND("退款"),
    SALARY("工资"),
    OTHER("其他");

    companion object {
        private val keywordMap = mapOf(
            FOOD to listOf("外卖", "饿了么", "美团", "餐饮", "午餐", "晚餐", "早餐", "咖啡", "奶茶",
                "美食", "餐厅", "食堂", "面包", "水果", "零食", "饮料", "买菜", "超市", "便利店",
                "盒马", "叮咚", "每日优鲜", "瑞幸", "星巴克", "肯德基", "麦当劳", "汉堡王"),
            TRANSPORT to listOf("滴滴", "打车", "地铁", "公交", "加油", "停车", "高铁", "机票",
                "出行", "高德", "骑行", "出租车", "网约车", "T3", "曹操", "ETC", "过路费"),
            SHOPPING to listOf("淘宝", "天猫", "京东", "拼多多", "商城", "购物", "百货", "网购",
                "唯品会", "得物", "抖音", "快手", "小红书", "严选", "小米", "华为", "苹果"),
            BILLS to listOf("水电", "燃气", "话费", "缴费", "物业", "宽带", "有线电视", "房租",
                "水费", "电费", "煤气", "供暖", "流量", "充值"),
            ENTERTAINMENT to listOf("电影", "视频", "音乐", "游戏", "KTV", "旅游", "酒店", "门票",
                "腾讯视频", "爱奇艺", "优酷", "B站", "网易云", "QQ音乐", "王者", "原神",
                "携程", "飞猪", "同程"),
            MEDICAL to listOf("医院", "药店", "药品", "医疗", "体检", "挂号", "诊所", "医保"),
            TRANSFER to listOf("转账", "红包", "汇款", "转出", "转入"),
            REFUND to listOf("退款", "已退款", "退票", "退货"),
            SALARY to listOf("工资", "奖金", "补贴", "绩效", "报销", "发放"),
        )

        fun detect(text: String): TransactionCategory {
            val lower = text.lowercase()
            for ((category, keywords) in keywordMap.entries) {
                if (keywords.any { lower.contains(it.lowercase()) }) return category
            }
            return OTHER
        }
    }
}
