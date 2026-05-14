# Activity 表新增字段使用说明

## 新增字段列表

已在 `Activity` 实体类和 `ActivityDTO` 中添加以下字段：

### 1. contactPhone (联系电话)
- **类型**: String
- **数据库类型**: VARCHAR(20)
- **说明**: 活动负责人的联系电话
- **示例**: "13800138000"

### 2. type (活动类型)
- **类型**: String
- **数据库类型**: VARCHAR(50)
- **说明**: 活动的类型分类
- **示例值**: 
  - "环保活动"
  - "社区服务"
  - "教育支援"
  - "医疗救助"
  - "文化传承"
  - "应急救援"

### 3. tags (标签列表)
- **类型**: String (JSON数组格式)
- **数据库类型**: TEXT
- **说明**: 活动的标签，用于分类和搜索
- **存储格式**: JSON字符串数组
- **示例**: `["环保","周末","户外","志愿者招募"]`

### 4. conditions (报名条件列表)
- **类型**: String (JSON数组格式)
- **数据库类型**: TEXT
- **说明**: 参与活动的报名条件或要求
- **存储格式**: JSON字符串数组
- **示例**: 
```json
[
  "年满18周岁",
  "身体健康",
  "有志愿服务经验优先",
  "能够全程参与活动"
]
```

### 5. feedbacks (志愿者反馈)
- **类型**: String (JSON数组格式)
- **数据库类型**: TEXT
- **说明**: 志愿者对活动的评价和反馈（可后续收集）
- **存储格式**: JSON对象数组
- **示例**:
```json
[
  {
    "userId": 1,
    "userName": "张三",
    "rating": 5,
    "comment": "非常有意义的活动，组织得很好！",
    "feedbackTime": "2024-01-15 18:30:00"
  },
  {
    "userId": 2,
    "userName": "李四",
    "rating": 4,
    "comment": "活动内容丰富，希望下次能增加互动环节",
    "feedbackTime": "2024-01-15 19:00:00"
  }
]
```

## 数据库迁移

如果数据库已经存在，请执行以下SQL脚本添加新字段：

```bash
mysql -u your_username -p volunteer_db < src/main/resources/sql/alter_activity_add_fields.sql
```

或者手动执行 `src/main/resources/sql/alter_activity_add_fields.sql` 文件中的SQL语句。

如果是新建数据库，直接执行 `src/main/resources/sql/init.sql` 即可包含所有字段。

## 使用示例

### 发布活动时设置新字段

```java
ActivityDTO activityDTO = new ActivityDTO();
activityDTO.setTitle("社区环保清洁活动");
activityDTO.setDescription("清理社区公园垃圾，美化环境");
activityDTO.setContactPhone("13800138000");
activityDTO.setType("环保活动");
activityDTO.setTags("[\"环保\",\"社区\",\"周末\"]");
activityDTO.setConditions("[\"年满16周岁\",\"自备清洁工具\",\"穿着舒适衣物\"]");
// feedbacks 通常在活动结束后收集
```

### 解析JSON字段

```java
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;

ObjectMapper mapper = new ObjectMapper();

// 解析标签
List<String> tags = mapper.readValue(activity.getTags(), List.class);

// 解析报名条件
List<String> conditions = mapper.readValue(activity.getConditions(), List.class);

// 解析反馈
List<Map<String, Object>> feedbacks = mapper.readValue(activity.getFeedbacks(), List.class);
```

### 前端使用示例（Vue/React）

```javascript
// 发布活动时
const activityData = {
  title: "社区环保清洁活动",
  description: "清理社区公园垃圾，美化环境",
  contactPhone: "13800138000",
  type: "环保活动",
  tags: JSON.stringify(["环保", "社区", "周末"]),
  conditions: JSON.stringify(["年满16周岁", "自备清洁工具"]),
  // ... 其他字段
};

// 获取活动后解析
const tags = JSON.parse(activity.tags);
const conditions = JSON.parse(activity.conditions);
const feedbacks = activity.feedbacks ? JSON.parse(activity.feedbacks) : [];
```

## 注意事项

1. **JSON格式验证**: 在保存 tags、conditions、feedbacks 字段前，建议进行JSON格式验证
2. **空值处理**: 这些字段允许为NULL，查询时需要做空值判断
3. **性能考虑**: tags 和 conditions 使用TEXT类型，如需频繁查询建议考虑索引或使用单独的关联表
4. **数据长度**: feedbacks 可能包含大量数据，注意数据库的TEXT字段大小限制（最大65KB）
5. **向后兼容**: 现有活动记录的这些字段为NULL，前端展示时需要处理默认值
