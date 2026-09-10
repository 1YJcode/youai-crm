const icon = (name, className = "") => `<svg class="${className}" aria-hidden="true"><use href="#i-${name}"></use></svg>`;

let customers = [
  { id: "YK-260813-001", name: "周雨桐", phone: "138 2167 8821", company: "天津澄途科技", source: "线上咨询", owner: "林夕", stage: "需求确认", level: "重点客户", amount: 128000, lastContact: "今天 10:32", nextFollow: "今天 16:00", city: "天津", tags: ["高意向", "企业版"], note: "关注多门店客户沉淀和销售过程管理，希望本周内完成方案评估。" },
  { id: "YK-260812-018", name: "陈嘉宇", phone: "186 1028 3706", company: "北京云杉商贸", source: "老客转介绍", owner: "陈晨", stage: "方案报价", level: "重点客户", amount: 86000, lastContact: "昨天 17:46", nextFollow: "8月18日", city: "北京", tags: ["连锁零售"], note: "需要 25 个坐席，已发送标准版报价单，等待财务确认。" },
  { id: "YK-260812-011", name: "宋晓婉", phone: "159 0062 5938", company: "上海栖木设计", source: "市场活动", owner: "周倩", stage: "初步沟通", level: "普通客户", amount: 32000, lastContact: "8月15日", nextFollow: "8月19日", city: "上海", tags: ["设计服务"], note: "正在比较不同 CRM 产品，重视使用体验和移动端。" },
  { id: "YK-260811-026", name: "王泽", phone: "139 2088 1649", company: "津南餐饮管理", source: "主动开发", owner: "赵磊", stage: "商务谈判", level: "重点客户", amount: 176000, lastContact: "8月14日", nextFollow: "明天 11:00", city: "天津", tags: ["多门店", "高价值"], note: "年度合同已进入法务审核，重点确认数据迁移服务范围。" },
  { id: "YK-260809-037", name: "刘思远", phone: "137 5219 0042", company: "星海教育咨询", source: "线上咨询", owner: "林夕", stage: "已成交", level: "重点客户", amount: 98000, lastContact: "8月13日", nextFollow: "8月25日", city: "天津", tags: ["已签约"], note: "已签约专业版，等待下周一启动实施培训。" },
  { id: "YK-260807-009", name: "张曼", phone: "177 0211 6380", company: "杭州行简文化", source: "市场活动", owner: "周倩", stage: "暂缓跟进", level: "潜在客户", amount: 25000, lastContact: "8月8日", nextFollow: "9月1日", city: "杭州", tags: ["低优先级"], note: "项目预算延后至下月，届时重新确认采购计划。" },
  { id: "YK-260806-014", name: "马骁", phone: "150 2206 7751", company: "启程汽车服务", source: "老客转介绍", owner: "陈晨", stage: "需求确认", level: "普通客户", amount: 56000, lastContact: "8月12日", nextFollow: "8月20日", city: "天津", tags: ["售后服务"], note: "希望整合现有呼叫系统，需要安排技术接口评估。" },
  { id: "YK-260805-021", name: "杜文静", phone: "131 9460 2218", company: "青岛屿见旅业", source: "线上咨询", owner: "赵磊", stage: "已流失", level: "普通客户", amount: 42000, lastContact: "8月9日", nextFollow: "-", city: "青岛", tags: ["价格敏感"], note: "客户本期选择价格更低的本地服务商，三个月后再次触达。" }
];

let orders = [
  { id: "SO20260817008", customer: "刘思远", product: "专业版 · 20 席位", amount: 98000, paid: 98000, status: "已支付", service: "待开通", owner: "林夕", created: "2026-08-17 09:42" },
  { id: "SO20260816023", customer: "王泽", product: "企业版 · 35 席位", amount: 176000, paid: 88000, status: "部分支付", service: "实施中", owner: "赵磊", created: "2026-08-16 16:18" },
  { id: "SO20260815017", customer: "陈嘉宇", product: "标准版 · 25 席位", amount: 86000, paid: 0, status: "待支付", service: "未开始", owner: "陈晨", created: "2026-08-15 13:06" },
  { id: "SO20260812006", customer: "马骁", product: "呼叫中心增值包", amount: 56000, paid: 56000, status: "已支付", service: "已开通", owner: "陈晨", created: "2026-08-12 11:27" },
  { id: "SO20260809031", customer: "宋晓婉", product: "标准版 · 8 席位", amount: 32000, paid: 0, status: "已取消", service: "未开始", owner: "周倩", created: "2026-08-09 15:44" }
];

let tasks = [
  { id: 1, title: "回访周雨桐，确认门店数量", customer: "周雨桐", owner: "林夕", due: "今天 16:00", type: "电话跟进", status: "today", done: false, priority: "紧急" },
  { id: 2, title: "为陈嘉宇更新 25 席位报价", customer: "陈嘉宇", owner: "陈晨", due: "今天 18:00", type: "发送资料", status: "today", done: false, priority: "高" },
  { id: 3, title: "安排启程汽车技术接口评估", customer: "马骁", owner: "陈晨", due: "明天 10:30", type: "会议", status: "upcoming", done: false, priority: "普通" },
  { id: 4, title: "跟进王泽合同法务意见", customer: "王泽", owner: "赵磊", due: "8月18日", type: "合同", status: "upcoming", done: false, priority: "高" },
  { id: 5, title: "补录上海栖木需求访谈纪要", customer: "宋晓婉", owner: "周倩", due: "昨天 17:00", type: "记录", status: "overdue", done: false, priority: "逾期" },
  { id: 6, title: "星海教育实施启动会", customer: "刘思远", owner: "林夕", due: "8月25日", type: "会议", status: "done", done: true, priority: "完成" }
];

let calls = [
  { id: 1, customerId: "YK-260813-001", customer: "周雨桐", phone: "138 2167 8821", direction: "呼出", status: "已接通", durationSeconds: 522, agent: "林夕", startedAt: new Date().toISOString(), started: "今天 10:32" }
];

let conversations = [
  { id: 1, customerId: "YK-260813-001", name: "周雨桐", company: "天津澄途科技", preview: "好的，下午四点可以电话沟通", time: "10:38", unread: 2, color: "#eaf3ff" },
  { id: 2, customerId: "YK-260812-018", name: "陈嘉宇", company: "北京云杉商贸", preview: "麻烦再发我一版报价单", time: "昨天", unread: 1, color: "#e7f8f3" },
  { id: 3, customerId: "YK-260809-037", name: "刘思远", company: "星海教育咨询", preview: "实施同事已经联系我了", time: "周五", unread: 0, color: "#fff5df" },
  { id: 4, customerId: "YK-260812-011", name: "宋晓婉", company: "上海栖木设计", preview: "移动端支持哪些功能？", time: "周四", unread: 3, color: "#f1efff" }
];

let invitations = [];
let ledgerAccounts = [];

const conversationMessages = new Map();
const defaultMessageTemplates = [
  { id: "welcome", name: "首次跟进", channel: "微信 / 短信", content: "您好，我是优爱的客户顾问，想和您确认一下当前的采购计划。方便时回复我即可。", updatedAt: "系统预置" },
  { id: "quote", name: "报价跟进", channel: "微信 / 短信", content: "您好，之前发送的方案和报价您看得怎么样？如果有需要调整的地方，我可以继续为您完善。", updatedAt: "系统预置" },
  { id: "meeting", name: "会议确认", channel: "微信 / 短信", content: "您好，提醒您我们约定的沟通时间即将开始。如时间需要调整，请提前告诉我。", updatedAt: "系统预置" }
];

function localDateValue(date) {
  const offset = date.getTimezoneOffset() * 60000;
  return new Date(date.getTime() - offset).toISOString().slice(0, 10);
}

const dashboardToday = new Date();
const dashboardMonthStart = new Date(dashboardToday.getFullYear(), dashboardToday.getMonth(), 1);

const customerTableColumnDefinitions = [
  { key: "id", label: "ID", defaultVisible: true, group: "基础信息" },
  { key: "customer", label: "客户姓名/昵称", defaultVisible: true, group: "基础信息" },
  { key: "gender", label: "性别", defaultVisible: true, group: "基础信息" },
  { key: "maritalStatus", label: "婚况", defaultVisible: true, group: "基础信息" },
  { key: "age", label: "年龄", defaultVisible: true, group: "基础信息" },
  { key: "education", label: "学历", defaultVisible: true, group: "基础信息" },
  { key: "income", label: "收入", defaultVisible: true, group: "基础信息" },
  { key: "level", label: "等级", defaultVisible: true, group: "基础信息" },
  { key: "city", label: "城市", defaultVisible: true, group: "基础信息" },
  { key: "followUpCount", label: "跟进次数", defaultVisible: true, group: "跟进与归属" },
  { key: "uncontactedDays", label: "未联系天数", defaultVisible: true, group: "跟进与归属" },
  { key: "owner", label: "归属人", defaultVisible: true, group: "跟进与归属" },
  { key: "inviter", label: "邀约人", defaultVisible: true, group: "跟进与归属" },
  { key: "collaborator", label: "协作人", defaultVisible: true, group: "跟进与归属" },
  { key: "servicePerson", label: "服务人", defaultVisible: true, group: "跟进与归属" },
  { key: "customerStatus", label: "客户状态", defaultVisible: false, group: "跟进与归属" },
  { key: "firstAllocationAt", label: "首次分配时间", defaultVisible: false, group: "跟进与归属" },
  { key: "lastFollowUpAt", label: "最后跟进", defaultVisible: false, group: "跟进与归属" },
  { key: "lastLoginAt", label: "最后登录", defaultVisible: false, group: "跟进与归属" },
  { key: "tags", label: "标签", defaultVisible: false, group: "基础信息" },
  { key: "source", label: "来源", defaultVisible: false, group: "基础信息" }
];

const defaultCustomerTableColumns = customerTableColumnDefinitions
  .filter(column => column.defaultVisible)
  .map(column => column.key);
const customerFixedColumnKeys = ["id"];
const customerSortableColumnKeys = new Set([
  "age", "education", "income", "city", "followUpCount", "uncontactedDays",
  "firstAllocationAt", "lastFollowUpAt", "lastLoginAt"
]);
const customerEducationSortOrder = ["小学", "初中", "高中", "中专", "大专", "本科", "硕士", "博士", "博士后"];

function storedCustomerTableColumns() {
  try {
    const stored = JSON.parse(localStorage.getItem("youke.crm.customerTableColumns") || "null");
    const allowed = new Set(customerTableColumnDefinitions.map(column => column.key));
    const selected = Array.isArray(stored) ? stored.filter(key => allowed.has(key)) : [];
    const visible = customerTableColumnDefinitions
      .filter(column => customerFixedColumnKeys.includes(column.key) || selected.includes(column.key))
      .map(column => column.key);
    return visible.length ? visible : [...defaultCustomerTableColumns];
  } catch (_) {
    return [...defaultCustomerTableColumns];
  }
}

const state = {
  view: location.hash.replace("#", "") || "dashboard",
  customerSearch: "",
  customerNameSearch: "",
  customerStage: "全部阶段",
  customerLevel: "全部等级",
  customerStatus: "全部状态",
  customerScene: "all",
  customerOwner: "全部负责人",
  customerOwnerSelection: { nodes: [] },
  customerOwnerCascadeOpen: false,
  customerOwnerSearch: "",
  customerOwnerExpandedNodes: ["store:youai-tianjin", "group:sales"],
  customerStartDate: "",
  customerEndDate: "",
  customerDateRangePickerOpen: false,
  customerDateRangeDraftStart: "",
  customerDateRangeDraftEnd: "",
  customerDateRangeViewMonth: "",
  customerDateRangePicking: "start",
  customerAdvancedDateRanges: {
    registration: { start: "", end: "" },
    lastLogin: { start: "", end: "" },
    firstAllocation: { start: "", end: "" },
    lastFollowUp: { start: "", end: "" }
  },
  customerAdvancedDraftDateRanges: {
    registration: { start: "", end: "" },
    lastLogin: { start: "", end: "" },
    firstAllocation: { start: "", end: "" },
    lastFollowUp: { start: "", end: "" }
  },
  customerAdvancedDatePickerOpen: false,
  customerAdvancedDatePickerField: "",
  customerAdvancedDatePickerDraftStart: "",
  customerAdvancedDatePickerDraftEnd: "",
  customerAdvancedDatePickerViewMonth: "",
  customerAdvancedDatePickerPicking: "start",
  customerUncontactedDays: "all",
  customerUncontactedDaysCustom: "",
  customerGender: "all",
  customerAgeMin: "",
  customerAgeMax: "",
  customerHeightMin: "",
  customerHeightMax: "",
  customerEducation: [],
  customerEducationMenuOpen: false,
  customerAdvancedDraftGender: "all",
  customerMaritalStatus: "all",
  customerAdvancedDraftAgeMin: "",
  customerAdvancedDraftAgeMax: "",
  customerAdvancedDraftHeightMin: "",
  customerAdvancedDraftHeightMax: "",
  customerAdvancedDraftEducation: [],
  customerAdvancedDraftMaritalStatus: "all",
  customerAdvancedOwnerSelection: { nodes: [] },
  customerAdvancedDraftOwnerSelection: { nodes: [] },
  customerAdvancedOwnerCascadeOpen: false,
  customerAdvancedOwnerSearch: "",
  customerAdvancedOwnerExpandedNodes: ["store:youai-tianjin", "group:sales"],
  customerAdvancedCollaboratorSelection: { nodes: [] },
  customerAdvancedDraftCollaboratorSelection: { nodes: [] },
  customerAdvancedCollaboratorCascadeOpen: false,
  customerAdvancedCollaboratorSearch: "",
  customerAdvancedCollaboratorExpandedNodes: ["store:youai-tianjin", "group:sales"],
  customerAdvancedDraftUncontactedDays: "all",
  customerAdvancedDraftUncontactedDaysCustom: "",
  customerAdvancedDialStatus: "all",
  customerAdvancedDraftDialStatus: "all",
  customerAvatarFilter: "all",
  customerAdvancedDraftAvatar: "all",
  customerScope: "all",
  customerPage: 1,
  customerPageSize: 20,
  customerSort: { key: "", direction: "" },
  customerHeaderModalOpen: false,
  customerVisibleColumns: storedCustomerTableColumns(),
  customerHeaderDraftColumns: [],
  selectedCustomerIds: [],
  sincereScope: "all",
  serviceScope: "all",
  serviceScenario: "全部",
  serviceAdvancedOpen: false,
  whiteboardTagModalOpen: false,
  whiteboardSelectedTags: [],
  whiteboardNameSearch: "",
  whiteboardStatus: "全部",
  whiteboardAdvancedOpen: false,
  customerAdvancedOpen: false,
  customerSection: "客户列表",
  customerDetailId: null,
  customerDetailTab: "profile",
  poolCustomers: [],
  importRows: [],
  importActiveId: null,
  importDetailId: null,
  importEditingRow: null,
  importSelectedRows: [],
  importSkipInvalid: false,
  importDetailStatusFilter: "全部状态",
  importStatusFilter: "全部状态",
  importHistory: JSON.parse(localStorage.getItem("youke.crm.importHistory") || "[]"),
  customerAuditLog: JSON.parse(localStorage.getItem("youke.crm.customerAuditLog") || "[]"),
  customerCollaborators: JSON.parse(localStorage.getItem("youke.crm.customerCollaborators") || "{}"),
  customerRegistrationEvents: {},
  customerAssignmentEvents: {},
  quickFilter: "全部客户",
  taskFilter: "全部",
  taskMode: "board",
  calendarDate: new Date(),
  orderKeyword: "",
  orderPaymentStatus: "全部状态",
  orderServiceStatus: "全部状态",
  orderSection: "订单列表",
  orderPage: 1,
  orderRefunds: JSON.parse(localStorage.getItem("youke.crm.orderRefunds") || "[]"),
  systemSection: "用户管理",
  systemUsers: [],
  financeSection: "财务概览",
  callFilter: "all",
  callKeyword: "",
  callNameKeyword: "",
  callStatusFilter: "全部",
  callDirectionFilter: "全部",
  callAiPreview: false,
  callSection: "通话记录",
  callTaskFilter: "all",
  callAgentFilter: "全部坐席",
  callReviews: JSON.parse(localStorage.getItem("youke.crm.callReviews") || "{}"),
  messageSection: "消息管理",
  notificationFilter: "all",
  notificationRead: JSON.parse(localStorage.getItem("youke.crm.notificationRead") || "{}"),
  messageTemplates: JSON.parse(localStorage.getItem("youke.crm.messageTemplates") || "null") || defaultMessageTemplates,
  messageTemplateSearch: "",
  editingTemplateId: null,
  activeConversationId: 1,
  dashboardTab: "经营概览",
  dashboardFilters: { store: "", from: localDateValue(dashboardMonthStart), to: localDateValue(dashboardToday) },
  analyticsSection: "邀约记录",
  invitationFilters: { inviter: "", customer: "", customerId: "", method: "", store: "", arrivalStatus: "", createdFrom: "", createdTo: "", scheduledFrom: "", scheduledTo: "", arrivalFrom: "", arrivalTo: "", customerType: "", arrivalText: "", orderNo: "", gender: "", onlyFirst: false },
  invitationPage: 1,
  invitationPageSize: 10,
  invitationModalOpen: false,
  ledgerFilters: { status: "", store: "", from: "", to: "" },
  ledgerModalOpen: false,
  range: "本月",
  unread: 6,
  backendOnline: false,
  dashboard: null,
  workspaceTabs: [],
  activeWorkspaceTabId: "",
  workspaceTabsLoaded: false,
  auth: {
    token: localStorage.getItem("youke.crm.accessToken") || "",
    user: null
  }
};

const customerStatusOptions = [
  "未注册",
  "未激活",
  "0类：新客户",
  "1类：未接通，待跟进",
  "2类：已接通，未深入沟通",
  "3类：意向会员，待确定见面时间",
  "4类：已确定见面时间",
  "5类：已见面，待确定后续",
  "6类：爽约，待再次沟通",
  "7类：高级会员",
  "放弃",
  "新升级会员",
  "无对象，待推荐",
  "推动见面指导",
  "撮合再见面",
  "深入交往，推动恋爱",
  "确认恋爱，转介绍",
  "结婚",
  "暂停"
];

const customerFollowUpTypes = ["到访", "电话", "微信", "短信", "外出", "其他"];

const customerPoolReasons = ["非单身", "接通挂", "强烈拒绝", "不是本人", "其他"];

const customerDialStatusOptions = [
  ["all", "全部"],
  ["callable", "可拨打"],
  ["unavailable", "不可拨打"]
];

const customerAvatarOptions = [
  ["all", "全部"],
  ["has", "有头像"],
  ["none", "无头像"]
];

const customerUncontactedDaysOptions = [
  ["all", "全部"],
  ["3", "3天"],
  ["5", "5天"],
  ["7", "7天"],
  ["custom", "自定义"]
];

const customerGenderOptions = [
  ["all", "请选择"],
  ["男", "男"],
  ["女", "女"]
];

const customerMaritalStatusOptions = [
  ["all", "全部"],
  ["未婚", "未婚"],
  ["离异", "离异"],
  ["丧偶", "丧偶"],
  ["未婚先育", "未婚先育"],
  ["离异未育", "离异未育"],
  ["离异带孩", "离异带孩"],
  ["离异不带孩", "离异不带孩"]
];

const customerEducationOptions = [
  ["all", "全部"],
  ["中专", "中专"],
  ["高中及以下", "高中及以下"],
  ["大专", "大专"],
  ["本科", "本科"],
  ["硕士", "硕士"],
  ["博士", "博士"]
];

const customerOwnerStoreOptions = [
  ["all", "所属人"],
  ["youai-tianjin", "优爱天津店"]
];

function customerOwnerAccountUsers() {
  const loadedUsers = state.systemUsers.length
    ? state.systemUsers
    : (state.auth.user ? [state.auth.user] : []);
  return loadedUsers
    .map(user => ({
      id: user.id || user.username || user.account,
      username: user.username || user.account || "",
      name: user.displayName || user.name || user.username || user.account || "",
      departmentCode: user.departmentCode || "",
      departmentName: user.departmentName || user.department || "",
      roles: user.roles || []
    }))
    .filter(user => user.name && user.name !== "公海" && user.name !== "白板")
    .filter((user, index, users) => users.findIndex(item => item.id === user.id || item.name === user.name) === index);
}

function customerOwnerIsAdminUser(user) {
  return (user.roles || []).some(role => {
    const code = typeof role === "string" ? role : role?.code;
    return ["ADMIN", "ROLE_ADMIN"].includes(String(code || "").toUpperCase());
  });
}

function customerOwnerEmployeeOptions() {
  return customerOwnerAccountUsers()
    .filter(user => !customerOwnerIsAdminUser(user))
    .map(user => [String(user.id), user.name]);
}

function customerOwnerStoreLabel(value) {
  return customerOwnerStoreOptions.find(([optionValue]) => optionValue === value)?.[1] || "所属人";
}

function customerOwnerTree() {
  const users = customerOwnerAccountUsers();
  const adminUsers = users.filter(customerOwnerIsAdminUser).map(user => ({
    id: `user:${user.id}`,
    label: user.name,
    owner: user.name,
    children: []
  }));
  const employees = users.filter(user => !customerOwnerIsAdminUser(user)).map(user => ({
    id: `user:${user.id}`,
    label: user.name,
    owner: user.name,
    children: []
  }));
  return {
    id: "store:youai-tianjin",
    label: "优爱天津店",
    children: [
      ...adminUsers,
      { id: "group:sales", label: "销售部", children: employees }
    ]
  };
}

function customerOwnerSelectionNodes(selection = {}) {
  if (Array.isArray(selection.nodes)) return selection.nodes;
  if (selection.employee && selection.employee !== "all") return [`employee:${selection.employee}`];
  if (selection.group && selection.group !== "all") return [`group:${selection.group}`];
  if (selection.store && selection.store !== "all") return [`store:${selection.store}`];
  return [];
}

function customerOwnerTreeFindNode(node, id) {
  if (node.id === id) return node;
  for (const child of node.children || []) {
    const match = customerOwnerTreeFindNode(child, id);
    if (match) return match;
  }
  return null;
}

function customerOwnerTreeDescendantIds(node) {
  return [node.id, ...(node.children || []).flatMap(child => customerOwnerTreeDescendantIds(child))];
}

function customerOwnerTreeContains(node, id) {
  return customerOwnerTreeDescendantIds(node).includes(id);
}

function customerOwnerTreeIsAncestor(parentId, childId) {
  const parent = customerOwnerTreeFindNode(customerOwnerTree(), parentId);
  return Boolean(parent && parentId !== childId && customerOwnerTreeContains(parent, childId));
}

function customerOwnerSelectionLabel(selection = {}) {
  const tree = customerOwnerTree();
  const nodes = customerOwnerSelectionNodes(selection);
  if (!nodes.length) return "所属人";
  const labels = nodes.map(id => customerOwnerTreeFindNode(tree, id)?.label || id).filter(Boolean);
  return labels.length === 1 ? labels[0] : `${labels[0]} 等${labels.length}项`;
}

function customerOwnerTreeNodeView(node, scope, selection, expandedNodes, searchText = "") {
  const isAdvanced = scope !== "main";
  const isCollaborator = scope === "advanced-collaborator";
  const nodeAttribute = scope === "main" ? "data-customer-owner-node" : (isCollaborator ? "data-customer-advanced-collaborator-node" : "data-customer-advanced-owner-node");
  const expandAttribute = scope === "main" ? "data-customer-owner-expand" : (isCollaborator ? "data-customer-advanced-collaborator-expand" : "data-customer-advanced-owner-expand");
  const selectedNodes = customerOwnerSelectionNodes(selection);
  const descendants = customerOwnerTreeDescendantIds(node);
  const hasSelectedDescendant = selectedNodes.some(id => descendants.includes(id) && id !== node.id);
  const query = String(searchText || "").trim().toLowerCase();
  const matches = !query || node.label.toLowerCase().includes(query) || (node.children || []).some(child => customerOwnerTreeNodeViewMatches(child, query));
  if (!matches) return "";
  const hasChildren = Boolean(node.children?.length);
  const expanded = hasChildren && (query || expandedNodes.includes(node.id));
  return `<div class="owner-cascade-tree-node ${hasSelectedDescendant ? "partial" : ""}">
    <div class="owner-cascade-tree-row"><button type="button" class="owner-cascade-expand ${hasChildren ? "" : "empty"}" ${hasChildren ? `${expandAttribute}="${escapeHtml(node.id)}" aria-expanded="${expanded ? "true" : "false"}"` : 'disabled aria-hidden="true"'}>${hasChildren ? "" : ""}</button><label><input type="checkbox" ${nodeAttribute}="${escapeHtml(node.id)}" ${selectedNodes.includes(node.id) ? "checked" : ""}><span>${escapeHtml(node.label)}</span></label></div>${expanded ? `<div class="owner-cascade-tree-children">${(node.children || []).map(child => customerOwnerTreeNodeView(child, scope, selection, expandedNodes, searchText)).join("")}</div>` : ""}
  </div>`;
}

function customerOwnerTreeNodeViewMatches(node, query) {
  return node.label.toLowerCase().includes(query) || (node.children || []).some(child => customerOwnerTreeNodeViewMatches(child, query));
}

function customerOwnerCascadeMenu(scope, selection = {}) {
  const isAdvanced = scope !== "main";
  const isCollaborator = scope === "advanced-collaborator";
  const searchText = isCollaborator ? state.customerAdvancedCollaboratorSearch : (isAdvanced ? state.customerAdvancedOwnerSearch : state.customerOwnerSearch);
  const expandedNodes = isCollaborator ? state.customerAdvancedCollaboratorExpandedNodes : (isAdvanced ? state.customerAdvancedOwnerExpandedNodes : state.customerOwnerExpandedNodes);
  return `<div class="owner-cascade-menu owner-cascade-tree-menu" role="menu" aria-label="选择所属人"><div class="owner-cascade-tree">${customerOwnerTreeNodeView(customerOwnerTree(), scope, selection, expandedNodes, searchText) || `<span class="owner-cascade-empty">未找到匹配的所属人</span>`}</div></div>`;
}

function customerOwnerCascadeControl(scope, selection = {}, open = false) {
  const isAdvanced = scope !== "main";
  const isCollaborator = scope === "advanced-collaborator";
  const toggleId = scope === "main" ? "customerOwnerToggle" : (isCollaborator ? "customerAdvancedCollaboratorToggle" : "customerAdvancedOwnerToggle");
  const tree = customerOwnerTree();
  const removeAttribute = scope === "main" ? "data-customer-owner-remove" : (isCollaborator ? "data-customer-advanced-collaborator-remove" : "data-customer-advanced-owner-remove");
  const chips = customerOwnerSelectionNodes(selection).map(id => {
    const label = customerOwnerTreeFindNode(tree, id)?.label || id;
    return `<span class="owner-cascade-chip">${escapeHtml(label)}<span role="button" tabindex="0" ${removeAttribute}="${escapeHtml(id)}" aria-label="移除${escapeHtml(label)}">×</span></span>`;
  }).join("");
  return `<div class="owner-cascade-filter ${open ? "open" : ""}"><button class="owner-cascade-toggle" id="${toggleId}" type="button" aria-haspopup="menu" aria-expanded="${open ? "true" : "false"}"><span class="owner-cascade-values">${chips || '<span class="owner-cascade-placeholder">所属人</span>'}</span><span class="owner-cascade-arrow" aria-hidden="true"></span></button>${open ? customerOwnerCascadeMenu(scope, selection) : ""}</div>`;
}

function customerOwnerToggleNodeSelection(selection, nodeId) {
  const tree = customerOwnerTree();
  const node = customerOwnerTreeFindNode(tree, nodeId);
  if (!node) return selection;
  const selectedNodes = customerOwnerSelectionNodes(selection);
  const descendants = customerOwnerTreeDescendantIds(node);
  if (selectedNodes.includes(nodeId)) return { nodes: selectedNodes.filter(id => !descendants.includes(id)) };
  const next = selectedNodes.filter(id => !descendants.includes(id) && !customerOwnerTreeIsAncestor(id, nodeId));
  return { nodes: [...next, nodeId] };
}

function customerOwnerHierarchyMatches(customer, selection = {}) {
  const selectedNodes = customerOwnerSelectionNodes(selection);
  if (!selectedNodes.length) return true;
  const tree = customerOwnerTree();
  return selectedNodes.some(id => {
    if (id === "store:youai-tianjin") return true;
    if (id === "group:sales") return (tree.children.find(node => node.id === id)?.children || []).some(node => node.owner === customer.owner);
    if (id.startsWith("user:")) return customerOwnerTreeFindNode(tree, id)?.owner === customer.owner;
    return Boolean(customerOwnerTreeFindNode(tree, id));
  });
}

function customerCollaboratorHierarchyMatches(customer, selection = {}) {
  const selectedNodes = customerOwnerSelectionNodes(selection);
  if (!selectedNodes.length) return true;
  const collaborators = Array.isArray(customer.collaborators)
    ? customer.collaborators
    : String(customer.collaborator || "").split("、").map(item => item.trim()).filter(Boolean);
  const tree = customerOwnerTree();
  return selectedNodes.some(id => {
    if (id === "store:youai-tianjin") return collaborators.length > 0;
    const node = customerOwnerTreeFindNode(tree, id);
    if (id === "group:sales") return collaborators.some(name => (node.children || []).some(child => child.owner === name));
    if (id.startsWith("user:")) return collaborators.includes(node?.owner);
    return false;
  });
}

const API_BASE = window.YOUKE_API_BASE || "http://127.0.0.1:8080/api";

async function apiRequest(path, options = {}) {
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), 10000);
  try {
    const headers = { "Content-Type": "application/json", ...(options.headers || {}) };
    if (state.auth.token && !headers.Authorization) headers.Authorization = `Bearer ${state.auth.token}`;
    const response = await fetch(`${API_BASE}${path}`, {
      ...options,
      signal: controller.signal,
      headers
    });
    if (response.status === 401 && path !== "/auth/login") {
      clearAuth();
      showLogin("登录已过期，请重新登录");
      throw new Error("登录已过期，请重新登录");
    }
    if (!response.ok) {
      const error = await response.json().catch(() => ({}));
      const fieldError = Object.values(error.fieldErrors || {})[0];
      throw new Error(fieldError || error.message || `请求失败 (${response.status})`);
    }
    if (response.status === 204) return null;
    return await response.json();
  } catch (error) {
    if (error.name === "AbortError") throw new Error("数据服务响应超时，请稍后重试");
    if (error instanceof TypeError) throw new Error("无法连接数据服务，请检查后端是否已启动");
    throw error;
  } finally {
    clearTimeout(timeout);
  }
}

function clearAuth() {
  state.auth.token = "";
  state.auth.user = null;
  localStorage.removeItem("youke.crm.accessToken");
}

function saveAuth(auth) {
  state.auth.token = auth.accessToken;
  state.auth.user = auth.user;
  localStorage.setItem("youke.crm.accessToken", auth.accessToken);
}

function isAdmin() {
  return (state.auth.user?.roles || []).some(role => {
    const code = typeof role === "string" ? role : role?.code;
    return ["ADMIN", "ROLE_ADMIN"].includes(String(code || "").toUpperCase());
  });
}

function updateAuthChrome() {
  const user = state.auth.user;
  const menu = document.querySelector("#userMenu");
  if (!user || !menu) return;
  menu.querySelector(".avatar").textContent = (user.displayName || user.username || "用").slice(0, 1);
  menu.querySelector(".user-name").textContent = user.displayName || user.username;
  const switchButton = document.querySelector("#accountSwitchButton");
  if (switchButton) switchButton.hidden = !isAdmin();
}

function createCaptcha() {
  const chars = "23456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz";
  return Array.from({ length: 4 }, () => chars[Math.floor(Math.random() * chars.length)]).join("");
}

function showLogin(message = "") {
  document.body.classList.remove("app-booting");
  const captcha = createCaptcha();
  document.querySelector(".topbar").hidden = true;
  document.querySelector(".help-rail").hidden = true;
  document.querySelector("#workspaceTabs").hidden = true;
  document.querySelector("#app").innerHTML = `<main class="login-screen"><section class="login-panel reference-login"><div class="reference-logo"><img src="logo-youai.png" alt="优爱 YOUAI"></div><div class="reference-name"><strong>客户经营管理平台</strong><span>让每一次客户沟通都有记录、有结果</span></div><div class="login-tabs"><button class="active" type="button">账号密码登录</button><button type="button" disabled title="即将开放">手机号登录</button></div><form class="login-form" id="loginForm"><label><span>登录账号</span><input name="username" autocomplete="username" required placeholder="请输入账号名，例如 admin"></label><label><span>登录密码</span><input name="password" type="password" autocomplete="current-password" required placeholder="请输入登录密码"></label><div class="captcha-row"><label><span>验证码</span><input name="captcha" required maxlength="4" autocomplete="off" placeholder="请输入验证码"></label><button type="button" class="captcha-code" id="refreshCaptcha" aria-label="刷新验证码" title="点击刷新验证码">${captcha}</button></div><label class="auto-login"><input type="checkbox" checked> <span>记住登录状态</span></label><p class="login-error" id="loginError" ${message ? "" : "hidden"}>${escapeHtml(message)}</p><button class="button primary" type="submit" id="loginSubmit">登录系统</button></form><button class="auth-switch" type="button" id="showRegister">没有账号？注册销售账号</button><p class="demo-account">演示管理员：admin / Admin@123</p><footer class="reference-footer">Copyright © 2026<br><span>优爱 YOUAI</span> 出品</footer></section></main>`;
  document.querySelector("#loginForm").addEventListener("submit", submitLogin);
  document.querySelector("#refreshCaptcha").addEventListener("click", () => showLogin());
  document.querySelector("#showRegister").addEventListener("click", showRegister);
  document.querySelector("#loginForm input").focus();
}

function showRegister(message = "") {
  document.querySelector(".topbar").hidden = true;
  document.querySelector(".help-rail").hidden = true;
  document.querySelector("#workspaceTabs").hidden = true;
  document.querySelector("#app").innerHTML = `<main class="login-screen register-panel"><section class="login-panel register-panel"><div class="login-brand"><span class="brand-mark">优</span><div><strong>优爱</strong><small>YOUAI</small></div></div><div class="login-copy"><p class="eyebrow">CREATE ACCOUNT</p><h1>注册销售账号</h1><p>新账号将加入销售一部，并使用销售顾问权限。</p></div><form class="login-form" id="registerForm"><label><span>登录账号</span><input name="username" autocomplete="username" minlength="3" maxlength="32" pattern="[A-Za-z][A-Za-z0-9_.-]*" required placeholder="例如：zhangsan"></label><label><span>显示名称</span><input name="displayName" autocomplete="name" minlength="2" maxlength="64" required placeholder="例如：张三"></label><label><span>手机号（选填）</span><input name="phone" inputmode="numeric" autocomplete="tel" pattern="1[3-9][0-9]{9}" placeholder="请输入 11 位手机号"></label><label><span>登录密码</span><input name="password" type="password" autocomplete="new-password" minlength="8" maxlength="72" required placeholder="至少 8 个字符"></label><label><span>确认密码</span><input name="confirmPassword" type="password" autocomplete="new-password" minlength="8" maxlength="72" required placeholder="再次输入密码"></label><p class="login-error" id="registerError" ${message ? "" : "hidden"}>${escapeHtml(message)}</p><button class="button primary" type="submit" id="registerSubmit">注册并进入系统</button></form><button class="auth-switch" type="button" id="showLogin">已有账号？返回登录</button></section></main>`;
  document.querySelector("#registerForm").addEventListener("submit", submitRegister);
  document.querySelector("#showLogin").addEventListener("click", () => showLogin());
  document.querySelector("#registerForm input").focus();
}

async function submitLogin(event) {
  event.preventDefault();
  const form = event.currentTarget;
  const submit = document.querySelector("#loginSubmit");
  const error = document.querySelector("#loginError");
  submit.disabled = true;
  error.hidden = true;
  try {
    const data = Object.fromEntries(new FormData(form));
    if (data.captcha !== document.querySelector("#refreshCaptcha")?.textContent.trim()) {
      error.textContent = "验证码不正确";
      error.hidden = false;
      submit.disabled = false;
      return;
    }
    const auth = await apiRequest("/auth/login", { method: "POST", body: JSON.stringify(data) });
    saveAuth(auth);
    showApp();
    await hydrateFromApi();
    toast(`欢迎回来，${auth.user.displayName || auth.user.username}`);
  } catch (requestError) {
    error.textContent = requestError.message.includes("请求失败") ? "账号或密码不正确" : requestError.message;
    error.hidden = false;
  } finally {
    submit.disabled = false;
  }
}

async function submitRegister(event) {
  event.preventDefault();
  const form = event.currentTarget;
  const submit = document.querySelector("#registerSubmit");
  const error = document.querySelector("#registerError");
  const data = Object.fromEntries(new FormData(form));
  error.hidden = true;
  if (data.password !== data.confirmPassword) {
    error.textContent = "两次输入的密码不一致";
    error.hidden = false;
    form.querySelector('[name="confirmPassword"]').focus();
    return;
  }
  submit.disabled = true;
  try {
    const auth = await apiRequest("/auth/register", { method: "POST", body: JSON.stringify({ username: data.username, displayName: data.displayName, phone: data.phone || "", password: data.password }) });
    saveAuth(auth);
    showApp();
    await hydrateFromApi();
    toast(`账号注册成功，欢迎 ${auth.user.displayName || auth.user.username}`);
  } catch (requestError) {
    error.textContent = requestError.message;
    error.hidden = false;
  } finally {
    submit.disabled = false;
  }
}

function showApp() {
  document.querySelector(".topbar").hidden = false;
  document.querySelector(".help-rail").hidden = false;
  document.querySelector("#workspaceTabs").hidden = false;
  restoreActiveWorkspaceTab();
  updateAuthChrome();
  bindHelpActions();
  render();
  requestAnimationFrame(() => document.body.classList.remove("app-booting"));
}

function bindHelpActions() {
  document.querySelector("#userHelpButton")?.addEventListener("click", () => {
    document.querySelector("#userPopover").hidden = true;
    openHelpModal();
  });
  document.querySelectorAll("[data-help]").forEach(button => button.addEventListener("click", () => {
    openHelpModal(button.dataset.help === "学习中心" ? "learning" : "help");
  }));
}

async function logout() {
  try { if (state.auth.token) await apiRequest("/auth/logout", { method: "POST" }); } catch (_) { /* local logout still succeeds */ }
  clearAuth();
  state.backendOnline = false;
  showLogin();
}

async function openAccountSwitcher() {
  if (!isAdmin()) {
    toast("只有管理员可以切换其他账号");
    return;
  }
  try {
    const users = await apiRequest("/auth/users");
    const select = document.querySelector("#accountSelect");
    select.innerHTML = users.map(user => `<option value="${escapeHtml(user.username)}">${escapeHtml(user.displayName)}（${escapeHtml(user.username)} · ${escapeHtml(user.departmentName)}）</option>`).join("");
    select.value = state.auth.user.username;
    document.querySelector("#accountSwitchHint").textContent = "切换后将以所选账号的权限访问系统。退出后可重新登录管理员账号。";
    document.querySelector("#accountBackdrop").hidden = false;
    document.body.style.overflow = "hidden";
    select.focus();
  } catch (error) {
    toast(`账号列表加载失败：${error.message}`);
  }
}

function closeAccountSwitcher() {
  document.querySelector("#accountBackdrop").hidden = true;
  document.body.style.overflow = "";
}

async function submitAccountSwitch(event) {
  event.preventDefault();
  const username = new FormData(event.currentTarget).get("username");
  const submit = event.currentTarget.querySelector("button[type=submit]");
  submit.disabled = true;
  try {
    const auth = await apiRequest(`/auth/switch?username=${encodeURIComponent(username)}`, { method: "POST" });
    saveAuth(auth);
    closeAccountSwitcher();
    showApp();
    await hydrateFromApi();
    toast(`已切换到 ${auth.user.displayName || auth.user.username}`);
  } catch (error) {
    toast(`账号切换失败：${error.message}`);
  } finally {
    submit.disabled = false;
  }
}

function formatPhone(value) {
  const digits = String(value || "").replace(/\D/g, "");
  return digits.length === 11 ? digits.replace(/(\d{3})(\d{4})(\d{4})/, "$1 $2 $3") : value;
}

function maskPhoneDisplay(value) {
  const text = String(value ?? "").trim();
  if (!text || /[*＊]/.test(text)) return text;
  const digits = text.replace(/\D/g, "");
  if (digits.length === 11) return `${digits.slice(0, 3)}****${digits.slice(-4)}`;
  if (digits.length <= 5) return "******";
  return `${digits.slice(0, 3)}****${digits.slice(-2)}`;
}

function maskWechatDisplay(value) {
  const text = String(value ?? "").trim();
  if (!text || /[*＊]/.test(text)) return text;
  if (text.length <= 2) return "******";
  return `${text.slice(0, 1)}****${text.slice(-1)}`;
}

function formatRelativeDate(value) {
  if (!value) return "-";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  const now = new Date();
  const tomorrow = new Date(now.getFullYear(), now.getMonth(), now.getDate() + 1);
  const sameDay = date.toDateString() === now.toDateString();
  const time = date.toLocaleTimeString("zh-CN", { hour: "2-digit", minute: "2-digit", hour12: false });
  if (sameDay) return `今天 ${time}`;
  if (date.toDateString() === tomorrow.toDateString()) return `明天 ${time}`;
  return `${date.getMonth() + 1}月${date.getDate()}日`;
}

function formatDateTime(value) {
  if (!value) return "-";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  const parts = new Intl.DateTimeFormat("zh-CN", {
    year: "numeric", month: "2-digit", day: "2-digit", hour: "2-digit", minute: "2-digit", hour12: false
  }).formatToParts(date).reduce((result, part) => ({ ...result, [part.type]: part.value }), {});
  return `${parts.year}-${parts.month}-${parts.day} ${parts.hour}:${parts.minute}`;
}

function toDateTimeLocal(value) {
  if (!value) return "";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "";
  const local = new Date(date.getTime() - date.getTimezoneOffset() * 60000);
  return local.toISOString().slice(0, 16);
}

function currentOwner() {
  return state.auth.user?.displayName || state.auth.user?.username || "林夕";
}

function ownerOptions(selected = currentOwner()) {
  // Owners are employees, never arbitrary historical customer owner strings.
  const owners = [...new Set([
    ...state.systemUsers.map(user => user.name || user.displayName || user.account),
    currentOwner(),
    selected
  ].filter(owner => owner && owner !== "全部负责人" && owner !== "公海"))];
  return owners.map(owner => `<option value="${escapeHtml(owner)}" ${owner === selected ? "selected" : ""}>${escapeHtml(owner)}</option>`).join("");
}

function customerAssignmentOwnerTreeView(node, selectedOwners, isRoot = false) {
  const hasChildren = Boolean(node.children?.length);
  if (!hasChildren) {
    return `<label class="resource-owner-option"><input type="checkbox" data-resource-owner-option value="${escapeHtml(node.owner || node.label)}" ${selectedOwners.includes(node.owner || node.label) ? "checked" : ""}><span>${escapeHtml(node.label)}</span></label>`;
  }
  return `<div class="resource-owner-tree-node"><div class="resource-owner-tree-row ${isRoot ? "root" : "branch"}"><span class="resource-owner-tree-marker" aria-hidden="true"></span><strong>${escapeHtml(node.label)}</strong></div><div class="resource-owner-tree-children">${node.children.map(child => customerAssignmentOwnerTreeView(child, selectedOwners)).join("")}</div></div>`;
}

function customerAssignmentOwnerControl(selected = "") {
  const selectedOwners = String(selected || "").split(/[、,，]/).map(value => value.trim()).filter(Boolean);
  const chips = selectedOwners.map(owner => `<span class="resource-owner-chip">${escapeHtml(owner)}<span role="button" tabindex="0" data-resource-owner-remove="${escapeHtml(owner)}" aria-label="移除${escapeHtml(owner)}">×</span></span>`).join("");
  const tree = customerOwnerTree();
  return `<div class="resource-owner-select" data-resource-owner-select>
    <input type="hidden" name="owner" value="${escapeHtml(selectedOwners.join("、"))}">
    <button type="button" class="resource-owner-control" data-resource-owner-control aria-haspopup="listbox" aria-expanded="false"><span class="resource-owner-chips">${chips || '<span class="resource-owner-placeholder">请选择接受对象</span>'}</span><span class="resource-owner-arrow" aria-hidden="true"></span></button>
    <div class="resource-owner-menu" data-resource-owner-menu role="listbox" aria-label="选择接受对象" hidden>${customerAssignmentOwnerTreeView(tree, selectedOwners, true) || '<span class="resource-owner-empty">暂无可分配的销售人员</span>'}</div>
  </div>`;
}

function syncCustomerAssignmentOwnerControl(control) {
  if (!control) return;
  const checked = [...control.querySelectorAll("[data-resource-owner-option]:checked")];
  const owners = checked.map(option => option.value);
  const hidden = control.querySelector("input[name=owner]");
  if (hidden) hidden.value = owners.join("、");
  const chips = control.querySelector(".resource-owner-chips");
  if (chips) chips.innerHTML = owners.length
    ? owners.map(owner => `<span class="resource-owner-chip">${escapeHtml(owner)}<span role="button" tabindex="0" data-resource-owner-remove="${escapeHtml(owner)}" aria-label="移除${escapeHtml(owner)}">×</span></span>`).join("")
    : '<span class="resource-owner-placeholder">请选择接受对象</span>';
}

function normalizeCustomer(customer) {
  const registrationCount = Math.max(1, Number(customer.registrationCount || 1));
  const storedCollaborators = state.customerCollaborators?.[customer.id];
  const backendCollaborators = String(customer.collaborator || "").split("、").map(item => item.trim()).filter(Boolean);
  const collaborators = Array.isArray(customer.collaborators)
    ? customer.collaborators.filter(Boolean)
    : backendCollaborators.length
      ? backendCollaborators
    : Array.isArray(storedCollaborators)
      ? storedCollaborators.filter(Boolean)
      : [customer.collaborator || storedCollaborators].filter(Boolean);
  return {
    ...customer,
    collaborators,
    collaborator: customer.collaborator || collaborators.join("、"),
    phone: formatPhone(customer.phone),
    amount: Number(customer.amount || 0),
    lastContact: formatRelativeDate(customer.lastContactAt || customer.lastContact),
    nextFollow: formatRelativeDate(customer.nextFollowAt || customer.nextFollow),
    tags: customer.tags || [],
    registrationCount,
    duplicateRegistration: registrationCount > 1
  };
}

function normalizeInvitation(row) {
  return { ...row, createdAt: row.createdAt || "", scheduledAt: row.scheduledAt || "", arrivalAt: row.arrivalAt || "" };
}
function normalizeLedger(row) { return { ...row, orderAmount: Number(row.orderAmount || 0), feeAmount: Number(row.feeAmount || 0), ledgerAmount: Number(row.ledgerAmount || 0) }; }

function normalizeTask(task) {
  return { ...task, due: formatRelativeDate(task.dueAt || task.due) };
}

function normalizeOrder(order) {
  return {
    ...order,
    amount: Number(order.amount || 0),
    paid: Number(order.paid || 0),
    performanceConfirmed: Boolean(order.performanceConfirmed),
    created: formatDateTime(order.createdAt || order.created)
  };
}

function normalizeRefund(refund) {
  return {
    ...refund,
    id: refund.id,
    orderId: refund.orderId || refund.orderNo,
    customer: refund.customer || refund.customerName,
    amount: Number(refund.amount || 0),
    createdAt: refund.createdAt || refund.created,
    reviewedAt: refund.reviewedAt || null
  };
}

function normalizeConversation(conversation) {
  return {
    ...conversation,
    time: formatRelativeDate(conversation.lastMessageAt),
    unread: Number(conversation.unread || 0),
    color: "#eaf3ff"
  };
}

function normalizeMessage(message) {
  return { ...message, outgoing: message.direction === "OUTBOUND", time: formatDateTime(message.sentAt).slice(11) };
}

function normalizeCall(call) {
  return { ...call, phone: formatPhone(call.phone), started: formatRelativeDate(call.startedAt), durationSeconds: Number(call.durationSeconds || 0) };
}

function normalizeMessageTemplate(template) {
  const systemTemplate = template.ownerUsername === "*";
  return {
    ...template,
    updatedAt: systemTemplate ? "系统预置" : (template.updatedAt ? formatDateTime(template.updatedAt) : "刚刚")
  };
}

function formatDuration(seconds) {
  const value = Math.max(0, Number(seconds || 0));
  const minutes = Math.floor(value / 60).toString().padStart(2, "0");
  return `${minutes}:${Math.floor(value % 60).toString().padStart(2, "0")}`;
}

function updateConnectionStatus(status, label) {
  const indicator = document.querySelector("#connectionStatus");
  indicator.className = `connection-status ${status}`;
  indicator.title = label;
  indicator.querySelector("span").textContent = status === "online" ? "数据库在线" : "服务不可用";
}

function requireBackend() {
  if (state.backendOnline) return;
  throw new Error("数据服务暂不可用，请恢复连接后重试");
}

async function hydrateFromApi() {
  if (!state.auth.token) return;
  try {
    const [customerData, taskData, orderData, dashboardData, callData, conversationData, poolData, refundData, templateData, notificationReadData, callReviewData, systemUserData, invitationData, ledgerData] = await Promise.all([
      apiRequest("/customers"),
      apiRequest("/tasks"),
      apiRequest("/orders"),
      apiRequest("/dashboard"),
      apiRequest("/calls"),
      apiRequest("/conversations"),
      apiRequest("/customers/pool"),
      apiRequest("/order-refunds"),
      apiRequest("/message-templates"),
      apiRequest("/notifications/read"),
      apiRequest("/call-reviews"),
      isAdmin() ? apiRequest("/auth/users") : Promise.resolve([]),
      apiRequest("/invitations"),
      apiRequest("/ledger-accounts")
    ]);
    customers = customerData.map(normalizeCustomer);
    tasks = taskData.map(normalizeTask);
    orders = orderData.map(normalizeOrder);
    state.orderRefunds = refundData.map(normalizeRefund);
    saveOrderRefunds();
    calls = callData.map(normalizeCall);
    state.messageTemplates = templateData.length ? templateData.map(normalizeMessageTemplate) : defaultMessageTemplates;
    state.notificationRead = Object.fromEntries(notificationReadData.map(id => [id, true]));
    state.callReviews = callReviewData;
    state.systemUsers = systemUserData.map(user => ({ id: user.id, account: user.username, name: user.displayName || user.username, gender: user.gender || "—", phone: user.phone || "—", storeDept: user.departmentName || "—", department: user.departmentName || "—", roles: user.roles || [] }));
    invitations = invitationData.map(normalizeInvitation);
    ledgerAccounts = ledgerData.map(normalizeLedger);
    localStorage.setItem("youke.crm.notificationRead", JSON.stringify(state.notificationRead));
    localStorage.setItem("youke.crm.callReviews", JSON.stringify(state.callReviews));
    conversations = conversationData.map(normalizeConversation);
    state.poolCustomers = poolData.map(normalizeCustomer);
    state.activeConversationId = conversations.some(item => item.id === state.activeConversationId) ? state.activeConversationId : conversations[0]?.id;
    if (state.activeConversationId) {
      const messages = await apiRequest(`/conversations/${state.activeConversationId}/messages`);
      conversationMessages.set(state.activeConversationId, messages.map(normalizeMessage));
    }
    state.unread = conversations.reduce((sum, item) => sum + item.unread, 0);
    const badge = document.querySelector("#messageBadge");
    badge.textContent = state.unread;
    badge.hidden = state.unread === 0;
    state.dashboard = dashboardData;
    state.backendOnline = true;
    updateConnectionStatus("online", "MySQL 数据服务已连接");
    render();
  } catch (error) {
    if (!state.auth.token) return;
    state.backendOnline = false;
    updateConnectionStatus("offline", `业务数据加载失败：${error.message}`);
    toast(`业务数据加载失败：${error.message}`);
  }
}

async function refreshDashboard() {
  requireBackend();
  const params = new URLSearchParams();
  if (state.dashboardFilters.store) params.set("store", state.dashboardFilters.store);
  if (state.dashboardFilters.from) params.set("from", state.dashboardFilters.from);
  if (state.dashboardFilters.to) params.set("to", state.dashboardFilters.to);
  state.dashboard = await apiRequest(`/dashboard?${params.toString()}`);
  render();
}

const viewMeta = {
  dashboard: { title: "经营概览", desc: "实时掌握客户增长、销售转化和团队待办。", eyebrow: "WORKSPACE OVERVIEW" },
  customers: { title: "客户管理", desc: "统一管理客户档案、跟进过程与商机状态。", eyebrow: "CUSTOMER MANAGEMENT" },
  tasks: { title: "跟进任务", desc: "按照优先级安排客户触达，避免遗漏关键节点。", eyebrow: "FOLLOW-UP TASKS" },
  calls: { title: "呼叫中心", desc: "查看通话记录、接通情况与坐席服务表现。", eyebrow: "CALL CENTER" },
  messages: { title: "消息中心", desc: "集中处理客户咨询，保持每次沟通都有记录。", eyebrow: "MESSAGE CENTER" },
  orders: { title: "订单管理", desc: "跟踪订单支付、服务开通与业绩确认进度。", eyebrow: "ORDER MANAGEMENT" },
  system: { title: "系统管理", desc: "管理账号、角色权限和业务基础配置。", eyebrow: "SYSTEM MANAGEMENT" },
  analytics: { title: "数据中心", desc: "分析获客渠道、销售漏斗和团队经营效率。", eyebrow: "BUSINESS ANALYTICS" },
  finance: { title: "财务系统", desc: "汇总收款、退款和订单应收情况。", eyebrow: "FINANCE SYSTEM" }
};

const workspaceDefaultSections = {
  dashboard: "经营概览",
  customers: "客户列表",
  tasks: "任务看板",
  calls: "通话记录",
  messages: "消息中心",
  orders: "订单列表",
  system: "用户管理",
  analytics: "邀约记录",
  finance: "财务概览"
};

function currentWorkspaceSection(view = state.view) {
  if (view === "dashboard") return state.dashboardTab;
  if (view === "customers") return state.customerSection;
  if (view === "tasks") return state.taskMode === "calendar" ? "日历视图" : state.taskMode === "activity" ? "跟进记录" : "任务看板";
  if (view === "calls") return state.callSection;
  if (view === "messages") return state.messageSection;
  if (view === "orders") return state.orderSection;
  if (view === "system") return state.systemSection;
  if (view === "analytics") return state.analyticsSection;
  if (view === "finance") return state.financeSection;
  return workspaceDefaultSections[view] || viewMeta[view]?.title || "首页";
}

function setWorkspaceSection(view, section) {
  if (view === "dashboard") state.dashboardTab = section;
  if (view === "customers") state.customerSection = section;
  if (view === "tasks") state.taskMode = section === "日历视图" ? "calendar" : section === "跟进记录" ? "activity" : "board";
  if (view === "calls") state.callSection = section;
  if (view === "messages") state.messageSection = section;
  if (view === "orders") state.orderSection = section;
  if (view === "system") state.systemSection = section;
  if (view === "analytics") state.analyticsSection = section;
  if (view === "finance") state.financeSection = section;
}

function workspaceTabLabel(view, section) {
  if (view === "customers" && state.importDetailId) return "导入详情";
  if (view === "customers" && state.customerDetailId) {
    const customer = [...customers, ...state.poolCustomers].find(item => String(item.id) === String(state.customerDetailId));
    return customer ? `${customer.name}[${customer.id}]` : `客户详情[${state.customerDetailId}]`;
  }
  return view === "dashboard" && section === "经营概览" ? "首页" : section || viewMeta[view]?.title || "功能页";
}

function workspaceTabId(view, section) {
  if (view === "customers" && state.importDetailId) return `customers:import-detail:${state.importDetailId}`;
  if (view === "customers" && state.customerDetailId) return `customers:detail:${state.customerDetailId}`;
  return `${view}:${section}`;
}

function loadWorkspaceTabs() {
  if (state.workspaceTabsLoaded) return;
  state.workspaceTabsLoaded = true;
  try {
    const stored = JSON.parse(localStorage.getItem("youke.crm.workspaceTabs") || "[]");
    state.workspaceTabs = Array.isArray(stored) ? stored.filter(tab => viewMeta[tab.view] && tab.section).map(tab => ({
      id: tab.importDetailId ? `customers:import-detail:${tab.importDetailId}` : tab.detailId ? `customers:detail:${tab.detailId}` : workspaceTabId(tab.view, tab.section),
      view: tab.view,
      section: tab.section,
      detailId: tab.detailId || null,
      importDetailId: tab.importDetailId || null,
      label: tab.importDetailId ? "导入详情" : tab.detailId ? (() => { const customer = [...customers, ...state.poolCustomers].find(item => String(item.id) === String(tab.detailId)); return customer ? `${customer.name}[${customer.id}]` : `客户详情[${tab.detailId}]`; })() : workspaceTabLabel(tab.view, tab.section)
    })) : [];
    state.activeWorkspaceTabId = localStorage.getItem("youke.crm.activeWorkspaceTab") || "";
  } catch (_) {
    state.workspaceTabs = [];
    state.activeWorkspaceTabId = "";
  }
}

function saveWorkspaceTabs() {
  localStorage.setItem("youke.crm.workspaceTabs", JSON.stringify(state.workspaceTabs));
  localStorage.setItem("youke.crm.activeWorkspaceTab", state.activeWorkspaceTabId);
}

function restoreActiveWorkspaceTab() {
  loadWorkspaceTabs();
  const tab = state.workspaceTabs.find(item => item.id === state.activeWorkspaceTabId);
  if (!tab || (location.hash && tab.view !== state.view)) return;
  const shouldOpenImportHistory = tab.importDetailId && location.hash === "#customers";
  const restoredTab = shouldOpenImportHistory
    ? state.workspaceTabs.find(item => item.view === "customers" && item.section === "客户导入" && !item.importDetailId)
    : tab;
  state.view = restoredTab?.view || tab.view;
  setWorkspaceSection(state.view, restoredTab?.section || tab.section);
  state.customerDetailId = restoredTab?.detailId || null;
  state.importDetailId = shouldOpenImportHistory ? null : (restoredTab?.importDetailId || null);
  state.activeWorkspaceTabId = restoredTab?.id || tab.id;
  saveWorkspaceTabs();
}

function ensureCurrentWorkspaceTab() {
  loadWorkspaceTabs();
  const section = currentWorkspaceSection();
  const id = workspaceTabId(state.view, section);
  let tab = state.workspaceTabs.find(item => item.id === id);
  if (!tab) {
    tab = { id, view: state.view, section, detailId: state.view === "customers" ? (state.customerDetailId || null) : null, importDetailId: state.view === "customers" ? (state.importDetailId || null) : null, label: workspaceTabLabel(state.view, section) };
    state.workspaceTabs.push(tab);
  } else if (state.view === "customers" && state.importDetailId && !tab.importDetailId) {
    tab.importDetailId = state.importDetailId;
  }
  state.activeWorkspaceTabId = id;
  saveWorkspaceTabs();
}

function renderWorkspaceTabs() {
  const bar = document.querySelector("#workspaceTabs");
  if (!bar) return;
  bar.innerHTML = state.workspaceTabs.map(tab => {
    const label = tab.importDetailId ? "导入详情" : tab.detailId ? (() => { const customer = [...customers, ...state.poolCustomers].find(item => String(item.id) === String(tab.detailId)); return customer ? `${customer.name}[${customer.id}]` : `客户详情[${tab.detailId}]`; })() : tab.label;
    tab.label = label;
    return `<div class="workspace-tab ${tab.id === state.activeWorkspaceTabId ? "active" : ""}" role="button" tabindex="0" data-workspace-tab="${escapeHtml(tab.id)}" ${tab.id === state.activeWorkspaceTabId ? 'aria-current="page"' : ""}><span>${escapeHtml(label)}</span>${tab.view === "dashboard" ? "" : `<button type="button" class="workspace-tab-close" data-close-workspace-tab="${escapeHtml(tab.id)}" aria-label="关闭 ${escapeHtml(label)}">${icon("close")}</button>`}</div>`;
  }).join("");
  bar.querySelector(`[data-workspace-tab="${CSS.escape(state.activeWorkspaceTabId)}"]`)?.scrollIntoView({ block: "nearest", inline: "nearest" });
}

function activateWorkspaceTab(id) {
  loadWorkspaceTabs();
  const tab = state.workspaceTabs.find(item => item.id === id);
  if (!tab) return;
  state.view = tab.view;
  setWorkspaceSection(tab.view, tab.section);
  state.customerDetailId = tab.detailId || null;
  state.importDetailId = tab.importDetailId || null;
  state.activeWorkspaceTabId = tab.id;
  saveWorkspaceTabs();
  location.hash = tab.view;
  render();
  window.scrollTo({ top: 0, behavior: "smooth" });
}

function closeWorkspaceTab(id) {
  loadWorkspaceTabs();
  const index = state.workspaceTabs.findIndex(tab => tab.id === id);
  if (index < 0) return;
  const wasActive = state.activeWorkspaceTabId === id;
  state.workspaceTabs.splice(index, 1);
  if (!state.workspaceTabs.length) {
    state.view = "dashboard";
    state.customerDetailId = null;
    state.importDetailId = null;
    setWorkspaceSection("dashboard", workspaceDefaultSections.dashboard);
    state.activeWorkspaceTabId = "";
    ensureCurrentWorkspaceTab();
    location.hash = "dashboard";
    render();
    return;
  }
  if (wasActive) {
    activateWorkspaceTab(state.workspaceTabs[Math.min(index, state.workspaceTabs.length - 1)].id);
  } else {
    saveWorkspaceTabs();
    renderWorkspaceTabs();
  }
}

function money(value) {
  return new Intl.NumberFormat("zh-CN", { style: "currency", currency: "CNY", maximumFractionDigits: 0 }).format(value);
}

function subnav(items, active, enabledItems = []) {
  return "";
}

function pageHeading(extra = "") {
  return extra ? `<header class="page-heading"><div class="heading-actions">${extra}</div></header>` : "";
}

function metric(label, value, unit, trend, tone, iconName, down = false) {
  const footer = trend ? `<div class="metric-trend ${down ? "down" : ""}">${icon(down ? "arrow-down" : "arrow-up")}<strong>${trend}</strong><span>较上月</span></div>` : `<div class="metric-trend"><span>实时业务数据</span></div>`;
  return `<article class="metric"><div class="metric-top"><span>${label}</span><span class="metric-icon ${tone}">${icon(iconName)}</span></div><div class="metric-value">${value}<small>${unit}</small></div>${footer}</article>`;
}

function dashboardReferenceView() {
  const summary = state.dashboard || {};
  const duration = Math.max(0, Number(summary.callDurationSeconds || 0));
  const durationText = [Math.floor(duration / 3600), Math.floor(duration % 3600 / 60), duration % 60].map(value => String(value).padStart(2, "0")).join(":");
  const cards = [
    ["新增客户数", summary.newCustomers ?? 0, "人", "customers"],
    ["跟进客户数", summary.followUpsToday ?? 0, "人", "tasks"],
    ["通话时长", durationText, "", "calls"],
    ["深沟次数", summary.deepCalls ?? 0, "次", "calls"],
    ["总库容", summary.totalCustomers ?? customers.length, "人", "customers"],
    ["实际到店客户数", summary.arrivedCustomers ?? 0, "人", "analytics"],
    ["成交客户数", summary.closedCustomers ?? 0, "人", "orders"]
  ];
  const stores = [...new Set(invitations.map(item => item.storeName || item.store).filter(Boolean))];
  const storeOptions = ["", ...stores].map(store => `<option value="${escapeHtml(store)}" ${state.dashboardFilters.store === store ? "selected" : ""}>${escapeHtml(store || "全部门店")}</option>`).join("");
  const emptyRank = `<tr><td colspan="6"><div class="home-rank-empty">当前筛选范围暂无数据</div></td></tr>`;
  const visitRows = (summary.visitRanking || []).map(row => `<tr><td><b class="rank-badge">${row.rank}</b></td><td><button class="rank-person" type="button" data-dashboard-owner="${escapeHtml(row.employee)}">${escapeHtml(row.employee)}</button></td><td>${escapeHtml(row.department)}</td><td>${row.arrivedCustomers}</td><td>${row.closedCustomers}</td><td>${Number(row.conversionRate || 0).toFixed(1)}%</td></tr>`).join("") || emptyRank;
  const salesRows = (summary.salesRanking || []).map(row => `<tr><td><b class="rank-badge">${row.rank}</b></td><td><button class="rank-person" type="button" data-dashboard-owner="${escapeHtml(row.employee)}">${escapeHtml(row.employee)}</button></td><td>${escapeHtml(row.department)}</td><td>${money(row.salesAmount || 0)}</td><td>${money(row.paidAmount || 0)}</td><td>${Number(row.completionRate || 0).toFixed(1)}%</td></tr>`).join("") || emptyRank;
  const wan = value => (Number(value || 0) / 10000).toFixed(2);
  const serviceCards = [["待开启服务会员", summary.pendingServiceCustomers ?? 0], ["在服务期内会员", summary.activeServiceCustomers ?? 0], ["即将到期会员", summary.expiringServiceCustomers ?? 0]];
  return `<section class="page dashboard-reference"><div class="page-content">
    <section class="home-dashboard-bar"><div class="home-dashboard-tabs"><button class="active" type="button">数据概览</button><button type="button" data-dashboard-target="analytics">客户到店登记</button></div><form id="dashboardFilterForm" class="home-dashboard-controls"><select id="dashboardStore">${storeOptions}</select><label><span>统计日期</span><input id="dashboardFrom" type="date" value="${state.dashboardFilters.from}"></label><i>—</i><input id="dashboardTo" type="date" value="${state.dashboardFilters.to}"><button class="button primary" type="submit">${icon("search")}查询</button><button class="button secondary" id="resetDashboardFilters" type="button">重置</button></form></section>
    <section class="home-metric-grid">${cards.map(([label, value, unit, target]) => `<button class="home-metric" type="button" data-dashboard-target="${target}"><h3>${label}</h3><strong>${value}<small>${unit}</small></strong><span>查看明细 →</span></button>`).join("")}</section>
    <section class="home-service-grid">${serviceCards.map(([label, value]) => `<button class="home-metric" type="button" data-dashboard-target="orders"><h3>${label}</h3><strong>${value}<small>人</small></strong><span>查看会员订单 →</span></button>`).join("")}<button class="home-metric home-money" type="button" data-dashboard-target="finance"><h3>实收金额</h3><strong>${wan(summary.paidAmount)}<small>万元</small></strong><span>查看收款流水 →</span></button><button class="home-metric home-money" type="button" data-dashboard-target="finance"><h3>退款金额</h3><strong>${wan(summary.refundAmount)}<small>万元</small></strong><span>查看退款明细 →</span></button></section>
    <section class="home-rank-grid"><article class="panel"><header class="panel-header"><h2>到店排行</h2><span>${escapeHtml(state.dashboardFilters.store || "全部门店")} / 个人排行</span></header><div class="table-wrap"><table class="data-table"><thead><tr><th>排名</th><th>员工</th><th>部门</th><th>到店客户数</th><th>成交人数</th><th>成交率</th></tr></thead><tbody>${visitRows}</tbody></table></div></article><article class="panel"><header class="panel-header"><h2>业绩排行</h2><span>销售部 / 个人排行</span></header><div class="table-wrap"><table class="data-table"><thead><tr><th>排名</th><th>员工</th><th>部门</th><th>销售额</th><th>回款额</th><th>完成率</th></tr></thead><tbody>${salesRows}</tbody></table></div></article></section>
  </div></section>`;
}

function dashboardView() {
  if (state.dashboardTab === "客户动态") return customerActivityView();
  if (state.dashboardTab === "团队业绩") return teamPerformanceView();
  return dashboardReferenceView();
  const summary = state.dashboard || {};
  const newCustomers = summary.newCustomers ?? 128;
  const followUpsToday = summary.followUpsToday ?? 36;
  const pipelineAmount = summary.pipelineAmount == null ? 86.4 : (Number(summary.pipelineAmount) / 10000).toFixed(1);
  const closedCustomers = summary.closedCustomers ?? 19;
  const salesAmount = summary.salesAmount == null ? 42.8 : (Number(summary.salesAmount) / 10000).toFixed(1);
  const pendingTasks = summary.pendingTasks ?? 5;
  const chart = `
    <div class="chart-wrap" aria-label="新增客户与成交客户趋势图">
      <svg viewBox="0 0 760 250" preserveAspectRatio="none" role="img">
        <defs><linearGradient id="areaBlue" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#1677ff" stop-opacity=".2"/><stop offset="1" stop-color="#1677ff" stop-opacity="0"/></linearGradient></defs>
        ${[28,76,124,172,220].map(y => `<line class="grid-line" x1="40" y1="${y}" x2="740" y2="${y}"/>`).join("")}
        <path d="M40 203 C90 196 109 151 157 160 S224 188 274 135 S352 158 397 114 S472 127 518 82 S593 103 636 58 S707 71 740 37 L740 220 L40 220 Z" fill="url(#areaBlue)" stroke="none"/>
        <path d="M40 203 C90 196 109 151 157 160 S224 188 274 135 S352 158 397 114 S472 127 518 82 S593 103 636 58 S707 71 740 37" fill="none" stroke="#1677ff" stroke-width="3"/>
        <path d="M40 216 C91 210 118 195 157 199 S228 208 274 179 S349 187 397 166 S472 172 518 147 S591 153 636 129 S703 140 740 113" fill="none" stroke="#0b9b84" stroke-width="2.5" stroke-dasharray="5 4"/>
        ${[40,157,274,397,518,636,740].map((x, i) => `<text class="axis-label" x="${x}" y="242" text-anchor="middle">${["8/01","8/04","8/07","8/10","8/13","8/16","8/17"][i]}</text>`).join("")}
      </svg>
      <div class="chart-tooltip">8月13日<strong>新增 24 人</strong></div>
    </div>`;
  const openTasks = tasks.filter(task => !task.done).slice(0, 4);
  return `<section class="page">
    ${subnav(["经营概览", "客户动态", "团队业绩"], state.dashboardTab, ["经营概览", "客户动态", "团队业绩"])}
    <div class="page-content">
      ${pageHeading(`<div class="segmented" data-range><button class="active" type="button">本月</button><button type="button">本季度</button><button type="button">本年</button></div><button class="button primary" type="button" data-add-customer>${icon("plus")}新增客户</button>`)}
      <section class="metric-grid">
        ${metric("新增客户", String(newCustomers), "人", "18.5%", "blue", "users")}
        ${metric("今日跟进", String(followUpsToday), "次", "12.0%", "green", "phone")}
        ${metric("商机金额", String(pipelineAmount), "万元", "23.7%", "amber", "wallet")}
        ${metric("成交客户", String(closedCustomers), "人", "9.2%", "purple", "target")}
        ${metric("销售额", String(salesAmount), "万元", "16.8%", "blue", "chart")}
        ${metric("待办任务", String(pendingTasks), "项", "2 项", "red", "task", true)}
      </section>
      <section class="dashboard-grid">
        <article class="panel"><header class="panel-header"><div class="panel-title"><h2>客户增长趋势</h2><span>过去 17 天</span></div><div class="legend"><span>新增客户</span><span>成交客户</span></div></header><div class="panel-body">${chart}</div></article>
        <article class="panel"><header class="panel-header"><div class="panel-title"><h2>销售漏斗</h2><span>本月实时</span></div><button class="button ghost" type="button" data-route="analytics">查看分析${icon("chevron")}</button></header><div class="panel-body">
          <div class="funnel-list">
            <div class="funnel-item"><span class="funnel-label">新线索</span><div class="funnel-track"><div class="funnel-fill" style="width:100%"></div></div><strong>320</strong></div>
            <div class="funnel-item"><span class="funnel-label">有效沟通</span><div class="funnel-track"><div class="funnel-fill" style="width:72%"></div></div><strong>231</strong></div>
            <div class="funnel-item"><span class="funnel-label">方案报价</span><div class="funnel-track"><div class="funnel-fill" style="width:38%"></div></div><strong>121</strong></div>
            <div class="funnel-item"><span class="funnel-label">成交签约</span><div class="funnel-track"><div class="funnel-fill" style="width:19%"></div></div><strong>61</strong></div>
          </div><div class="funnel-summary"><div><span>整体转化率</span><strong>19.1%</strong></div><div><span>平均成交周期</span><strong>18<small> 天</small></strong></div></div>
        </div></article>
      </section>
      <section class="dashboard-grid lower">
        <article class="panel"><header class="panel-header"><div class="panel-title"><h2>今日待办</h2><span>${openTasks.length} 项未完成</span></div><button class="button ghost" type="button" data-route="tasks">全部任务${icon("chevron")}</button></header><ul class="task-list">${openTasks.map(task => taskRow(task)).join("")}</ul></article>
        <article class="panel"><header class="panel-header"><div class="panel-title"><h2>销售业绩排行</h2><span>本月回款</span></div><span class="pill blue">团队目标 68%</span></header><div class="rank-list">
          ${[["林夕","销售一部",86,128600],["赵磊","大客户部",72,107900],["陈晨","销售一部",61,91600],["周倩","销售二部",47,70200]].map((item, i) => `<div class="rank-row"><span class="rank-index">${i + 1}</span><div class="person"><span class="person-avatar">${item[0][0]}</span><span class="person-copy"><strong>${item[0]}</strong><span>${item[1]}</span></span></div><div class="mini-bar"><span style="width:${item[2]}%"></span></div><span class="rank-value">${money(item[3])}</span></div>`).join("")}
        </div></article>
      </section>
    </div>
  </section>`;
}

function customerActivityRows() {
  const rows = [];
  state.customerAuditLog.forEach(item => rows.push(item));
  customers.forEach(customer => rows.push({ customerId: customer.id, customer: customer.name, type: "客户资料", detail: `客户阶段为“${customer.stage}”，负责人 ${customerOwnerDisplay(customer.owner)}`, owner: customerOwnerDisplay(customer.owner), at: customer.lastContactAt || customer.lastContact }));
  tasks.forEach(task => rows.push({ customerId: task.customerId, taskId: task.id, customer: task.customer, type: "跟进任务", detail: `${task.title}${task.customerStatus ? `（客户状态：${task.customerStatus}）` : ""}`, owner: task.owner, at: task.followedAt || task.createdAt || task.updatedAt || task.dueAt || task.due }));
  calls.forEach(call => rows.push({ customerId: call.customerId, customer: call.customer, type: "通话记录", detail: `${call.direction} · ${call.status} · ${formatDuration(call.durationSeconds)}`, owner: call.agent || call.owner, at: call.startedAt || call.started }));
  conversations.forEach(conversation => rows.push({ customerId: conversation.customerId, customer: conversation.name, type: "客户消息", detail: conversation.preview, owner: conversation.owner || "-", at: conversation.lastMessageAt || conversation.time }));
  return rows.sort((a, b) => new Date(b.at || 0) - new Date(a.at || 0));
}

function recordCustomerActivity({ customerId, customer, type, detail, owner }) {
  const item = { id: `audit-${Date.now()}-${Math.random().toString(36).slice(2, 7)}`, customerId, customer, type, detail, owner: owner || currentOwner(), at: new Date().toISOString() };
  state.customerAuditLog = [item, ...state.customerAuditLog].slice(0, 2000);
  localStorage.setItem("youke.crm.customerAuditLog", JSON.stringify(state.customerAuditLog));
}

function customerActivityView() {
  const rows = customerActivityRows();
  return `<section class="page">
    ${subnav(["经营概览", "客户动态", "团队业绩"], state.dashboardTab, ["经营概览", "客户动态", "团队业绩"])}
    <div class="page-content">
      <header class="page-heading"><div class="heading-actions"><button class="button secondary" id="exportCustomerActivity">${icon("download")}导出动态</button><button class="button primary" data-add-customer>${icon("plus")}新增客户</button></div></header>
      <section class="metric-grid">${metric("动态总数", String(rows.length), "条", "", "blue", "clock")}${metric("客户档案", String(customers.length), "个", "", "green", "users")}${metric("未完成任务", String(tasks.filter(task => !task.done).length), "项", "", "amber", "task")}${metric("通话记录", String(calls.length), "条", "", "purple", "phone")}${metric("会话消息", String(conversations.length), "个", "", "blue", "message")}${metric("今日客户", String(customers.filter(customer => customer.nextFollow.includes("今天")).length), "个", "", "red", "target")}</section>
      <section class="data-panel"><div class="data-toolbar"><div class="panel-title"><h2>最近动态</h2><span>按时间倒序排列</span></div><span class="pill gray">${rows.length} 条记录</span></div><div class="table-wrap"><table class="data-table"><thead><tr><th>客户</th><th>动态类型</th><th>动态内容</th><th>负责人</th><th>发生时间</th><th>操作</th></tr></thead><tbody>${rows.map(row => `<tr ${row.customerId ? `data-customer-id="${escapeHtml(row.customerId)}"` : ""}><td><strong>${escapeHtml(row.customer || "-")}</strong></td><td><span class="pill blue">${escapeHtml(row.type)}</span></td><td>${escapeHtml(row.detail || "-")}</td><td>${escapeHtml(row.owner || "-")}</td><td>${escapeHtml(formatRelativeDate(row.at))}</td><td>${row.customerId ? `<button class="button ghost" data-open-customer="${escapeHtml(row.customerId)}">查看客户${icon("chevron")}</button>` : "-"}</td></tr>`).join("")}</tbody></table></div></section>
    </div>
  </section>`;
}

function teamPerformanceView() {
  const owners = [...new Set([...customers.map(customer => customer.owner), ...orders.map(order => order.owner)].filter(owner => owner && !["公海", "白板"].includes(owner)))];
  const rows = owners.map(owner => {
    const ownerOrders = orders.filter(order => order.owner === owner);
    const paid = ownerOrders.reduce((sum, order) => sum + order.paid, 0);
    const amount = ownerOrders.reduce((sum, order) => sum + order.amount, 0);
    const ownerTasks = tasks.filter(task => task.owner === owner);
    return { owner, customers: customers.filter(customer => customer.owner === owner).length, paid, amount, completion: ownerTasks.length ? Math.round(ownerTasks.filter(task => task.done).length / ownerTasks.length * 100) : 0 };
  }).sort((a, b) => b.paid - a.paid);
  const maxPaid = Math.max(1, ...rows.map(row => row.paid));
  return `<section class="page">
    ${subnav(["经营概览", "客户动态", "团队业绩"], state.dashboardTab, ["经营概览", "客户动态", "团队业绩"])}
    <div class="page-content">
      <section class="data-panel"><div class="data-toolbar"><div class="panel-title"><h2>负责人排行</h2><span>实时业务数据</span></div></div><div class="table-wrap"><table class="data-table"><thead><tr><th>排名</th><th>负责人</th><th>客户数</th><th>订单额</th><th>已回款</th><th>任务完成率</th></tr></thead><tbody>${rows.map((row, index) => `<tr><td><span class="rank-index">${index + 1}</span></td><td><div class="customer-cell"><span class="person-avatar">${escapeHtml(row.owner[0])}</span><strong>${escapeHtml(row.owner)}</strong></div></td><td>${row.customers}</td><td>${money(row.amount)}</td><td><div class="team-progress"><span style="width:${Math.round(row.paid / maxPaid * 100)}%"></span></div><strong>${money(row.paid)}</strong></td><td><span class="pill ${row.completion >= 70 ? "green" : "amber"}">${row.completion}%</span></td></tr>`).join("")}</tbody></table></div></section>
    </div>
  </section>`;
}

function exportCustomerActivity() {
  const header = ["客户", "动态类型", "动态内容", "负责人", "发生时间"];
  const csv = "\ufeff" + [header, ...customerActivityRows().map(row => [row.customer, row.type, row.detail, row.owner, formatRelativeDate(row.at)])].map(row => row.map(value => `"${String(value ?? "").replaceAll('"', '""')}"`).join(",")).join("\n");
  const link = document.createElement("a"); link.href = URL.createObjectURL(new Blob([csv], { type: "text/csv;charset=utf-8" })); link.download = "优客云-客户动态.csv"; link.click(); setTimeout(() => URL.revokeObjectURL(link.href), 0);
  toast(`已导出 ${customerActivityRows().length} 条客户动态`);
}

function exportFollowUpRecords() {
  const rows = customerActivityRows().filter(row => ["跟进任务", "通话记录", "客户消息"].includes(row.type));
  const header = ["客户", "记录类型", "跟进内容", "负责人", "发生时间"];
  const csv = "\ufeff" + [header, ...rows.map(row => [row.customer, row.type, row.detail, row.owner, formatRelativeDate(row.at)])]
    .map(row => row.map(value => `"${String(value ?? "").replaceAll('"', '""')}"`).join(",")).join("\n");
  const link = document.createElement("a");
  link.href = URL.createObjectURL(new Blob([csv], { type: "text/csv;charset=utf-8" }));
  link.download = "优客云-跟进记录.csv";
  link.click();
  setTimeout(() => URL.revokeObjectURL(link.href), 0);
  toast(`已导出 ${rows.length} 条跟进记录`);
}

function taskRow(task) {
  return `<li class="task-row ${task.done ? "done" : ""}" data-task-id="${task.id}"><button class="task-check" type="button" aria-label="${task.done ? "标记未完成" : "标记完成"}">${icon("check")}</button><div><div class="task-name">${task.title}</div><div class="task-meta">${task.customer} · ${task.type} · ${task.owner}</div></div><span class="task-due ${task.status === "overdue" ? "overdue" : ""}">${icon("clock")}${task.due}</span></li>`;
}

function taskDateKey(task) {
  const value = task.dueAt || task.due;
  if (!value) return "";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "";
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`;
}

function calendarDateKey(date) {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`;
}

function calendarView() {
  const cursor = new Date(state.calendarDate);
  const year = cursor.getFullYear();
  const month = cursor.getMonth();
  const first = new Date(year, month, 1);
  const startOffset = (first.getDay() + 6) % 7;
  const todayKey = calendarDateKey(new Date());
  const cells = Array.from({ length: 42 }, (_, index) => {
    const date = new Date(year, month, index - startOffset + 1);
    const key = calendarDateKey(date);
    const inMonth = date.getMonth() === month;
    const dayTasks = tasks.filter(task => taskDateKey(task) === key);
    return `<div class="calendar-day ${inMonth ? "" : "outside"} ${key === todayKey ? "today" : ""}"><div class="calendar-day-header"><span>${date.getDate()}</span>${dayTasks.length ? `<small>${dayTasks.length} 项</small>` : ""}</div><div class="calendar-events">${dayTasks.map(task => `<button class="calendar-event ${task.done ? "completed" : ""}" type="button" data-task-card="${task.id}" title="${escapeHtml(task.title)}"><span class="calendar-event-dot"></span><span>${escapeHtml(task.title)}</span></button>`).join("")}</div></div>`;
  }).join("");
  const monthLabel = `${year} 年 ${month + 1} 月`;
  return `<section class="data-panel calendar-panel"><div class="data-toolbar calendar-toolbar"><div class="panel-title"><h2>${monthLabel}</h2><span>按任务截止时间查看安排</span></div><div class="calendar-controls"><button class="icon-button" type="button" data-calendar-shift="-1" aria-label="上个月">${icon("chevron-left")}</button><button class="button secondary" type="button" data-calendar-today>今天</button><button class="icon-button" type="button" data-calendar-shift="1" aria-label="下个月">${icon("chevron-right")}</button></div></div><div class="calendar-weekdays">${["周一", "周二", "周三", "周四", "周五", "周六", "周日"].map(day => `<span>${day}</span>`).join("")}</div><div class="calendar-grid">${cells}</div></section>`;
}

function followUpRecordsView() {
  const records = customerActivityRows().filter(row => ["跟进任务", "通话记录", "客户消息"].includes(row.type));
  return `<section class="data-panel activity-panel"><div class="data-toolbar"><div class="panel-title"><h2>跟进记录</h2><span>汇总任务、通话和消息中的客户触达记录</span></div><div class="inline-actions"><span class="pill gray">${records.length} 条记录</span><button class="button secondary" type="button" id="exportFollowUpRecords">${icon("download")}导出记录</button></div></div><div class="table-wrap">${records.length ? `<table class="data-table"><thead><tr><th>客户</th><th>记录类型</th><th>跟进内容</th><th>负责人</th><th>发生时间</th><th>操作</th></tr></thead><tbody>${records.map(row => `<tr><td><strong>${escapeHtml(row.customer || "-")}</strong></td><td><span class="pill ${row.type === "通话记录" ? "green" : row.type === "客户消息" ? "blue" : "amber"}">${escapeHtml(row.type)}</span></td><td>${escapeHtml(row.detail || "-")}</td><td>${escapeHtml(row.owner || "-")}</td><td>${escapeHtml(formatRelativeDate(row.at))}</td><td>${row.taskId ? `<button class="button ghost" type="button" data-task-record="${row.taskId}">查看任务${icon("chevron")}</button>` : row.customerId ? `<button class="button ghost" type="button" data-open-customer="${escapeHtml(row.customerId)}">查看客户${icon("chevron")}</button>` : "-"}</td></tr>`).join("")}</tbody></table>` : `<div class="empty-state"><span class="empty-icon">${icon("clock")}</span><strong>暂无跟进记录</strong><p>完成任务、记录通话或发送消息后，记录会自动出现在这里。</p><button class="button primary" type="button" id="newTaskFromRecords">${icon("plus")}新建跟进任务</button></div>`}</div></div></section>`;
}

function customerPill(stage) {
  const tones = { "初步沟通": "gray", "需求确认": "blue", "方案报价": "amber", "商务谈判": "purple", "已成交": "green", "暂缓跟进": "gray", "已流失": "red" };
  return `<span class="pill ${tones[stage] || "gray"}">${stage}</span>`;
}

function parseCustomerDate(value) {
  if (value instanceof Date) return Number.isNaN(value.getTime()) ? null : value;
  const text = String(value || "").trim();
  if (!text) return null;
  const parsed = new Date(text);
  if (!Number.isNaN(parsed.getTime())) return parsed;

  const now = new Date();
  const relative = text.match(/^(今天|昨天|前天)(?:\s+(\d{1,2}):(\d{2}))?/);
  if (relative) {
    const daysAgo = { "今天": 0, "昨天": 1, "前天": 2 }[relative[1]];
    const date = new Date(now.getFullYear(), now.getMonth(), now.getDate() - daysAgo,
      relative[2] ? Number(relative[2]) : 0, relative[3] ? Number(relative[3]) : 0);
    return date;
  }

  const monthDay = text.match(/^(\d{1,2})月(\d{1,2})日/);
  if (monthDay) return new Date(now.getFullYear(), Number(monthDay[1]) - 1, Number(monthDay[2]));
  return null;
}

function filteredCustomers(options = {}) {
  const includeWhiteboard = options.includeWhiteboard === true;
  const query = state.customerSearch.trim().toLowerCase();
  const nameQuery = state.customerNameSearch.trim().toLowerCase();
  const uncontactedDaysThreshold = selectedCustomerUncontactedDaysThreshold();
  const ageRange = selectedCustomerAgeRange();
  const heightRange = selectedCustomerHeightRange();
  const selectedEducations = selectedCustomerEducations();
  const advancedDateRanges = state.customerAdvancedDateRanges;
  return customers.filter(customer => {
    const queryMatch = !query || [customer.name, customer.phone, customer.company, customer.id].join(" ").toLowerCase().includes(query);
    const nameMatch = !nameQuery || [customer.name, customer.note, customer.remark].join(" ").toLowerCase().includes(nameQuery);
    const stageMatch = state.customerStage === "全部阶段" || customer.stage === state.customerStage;
    const levelMatch = state.customerLevel === "全部等级" || customer.level === state.customerLevel;
    const customerStatus = customer.statusCategory || customer.customerStatus || customer.stage;
    const statusMatch = state.customerStatus === "全部状态" || customerStatus === state.customerStatus;
    const ownerMatch = state.customerOwner === "全部负责人" || customer.owner === state.customerOwner;
    const ownerHierarchyMatch = customerOwnerHierarchyMatches(customer, state.customerOwnerSelection);
    const advancedOwnerMatch = customerOwnerHierarchyMatches(customer, state.customerAdvancedOwnerSelection);
    const advancedCollaboratorMatch = customerCollaboratorHierarchyMatches(customer, state.customerAdvancedCollaboratorSelection);
    const hasAvatar = customerHasAvatar(customer);
    const avatarMatch = state.customerAvatarFilter === "all"
      || (state.customerAvatarFilter === "has" ? hasAvatar : !hasAvatar);
    const genderMatch = state.customerGender === "all" || customer.gender === state.customerGender;
    const maritalStatusMatch = state.customerMaritalStatus === "all" || customer.maritalStatus === state.customerMaritalStatus;
    const educationMatch = !selectedEducations.length || selectedEducations.includes(customer.education);
    const allocationDate = String(customer.lastAllocationAt || customer.firstAllocationAt || customer.createdAt || customer.created || "").slice(0, 10);
    const startDateMatch = !state.customerStartDate || allocationDate >= state.customerStartDate;
    const endDateMatch = !state.customerEndDate || allocationDate <= state.customerEndDate;
    const uncontactedDaysMatch = uncontactedDaysThreshold === null || customerUncontactedDays(customer) >= uncontactedDaysThreshold;
    const ageMatch = customerMatchesAgeRange(customer, ageRange);
    const heightMatch = customerMatchesHeightRange(customer, heightRange);
    const registrationDateMatch = customerMatchesDateRangeValue(customer.createdAt || customer.created, advancedDateRanges.registration);
    const lastLoginDateMatch = customerMatchesDateRangeValue(customer.lastLoginAt || customer.lastLogin, advancedDateRanges.lastLogin);
    const firstAllocationDateMatch = customerMatchesDateRangeValue(customer.firstAllocationAt || customer.firstAssignedAt || customer.assignedAt || customer.lastAllocationAt, advancedDateRanges.firstAllocation);
    const lastFollowUpDateMatch = customerMatchesDateRangeValue(customer.lastContactAt || customer.lastContact || customer.updatedAt, advancedDateRanges.lastFollowUp);
    const quickMatch = state.quickFilter === "全部客户" || (state.quickFilter === "重点客户" && customer.level === "重点客户") || (state.quickFilter === "今日待跟进" && customer.nextFollow.includes("今天")) || (state.quickFilter === "即将成交" && ["方案报价", "商务谈判"].includes(customer.stage));
    const sceneMatch = customerMatchesScene(customer, state.customerScene);
    const scopeMatch = state.customerScope === "all" || (state.customerScope === "mine" && customer.owner === currentOwner());
    const ownerScopeMatch = includeWhiteboard ? customer.owner !== "公海" : !["公海", "白板"].includes(customer.owner);
    return ownerScopeMatch && queryMatch && nameMatch && stageMatch && levelMatch && statusMatch && ownerMatch && ownerHierarchyMatch && advancedOwnerMatch && advancedCollaboratorMatch && avatarMatch && genderMatch && maritalStatusMatch && educationMatch && ageMatch && heightMatch && registrationDateMatch && lastLoginDateMatch && firstAllocationDateMatch && lastFollowUpDateMatch && startDateMatch && endDateMatch && uncontactedDaysMatch && quickMatch && sceneMatch && scopeMatch;
  });
}

function customerHasAvatar(customer) {
  const avatar = customer.avatar
    ?? customer.avatarUrl
    ?? customer.headImage
    ?? customer.headImg
    ?? customer.profileImage
    ?? customer.photo
    ?? customer.portrait
    ?? customer.image
    ?? customer.hasAvatar;
  if (typeof avatar === "boolean") return avatar;
  if (typeof avatar === "number") return avatar > 0;
  const value = String(avatar ?? "").trim().toLowerCase();
  if (["false", "0", "no", "none", "无"].includes(value)) return false;
  return Boolean(value);
}

function customerFollowUpCount(customer) {
  return tasks.filter(task => task.customer === customer.name).length
    + calls.filter(call => call.customerId === customer.id).length;
}

function daysSince(value) {
  const date = value ? new Date(value) : null;
  if (!date || Number.isNaN(date.getTime())) return Number.POSITIVE_INFINITY;
  return Math.max(0, Math.floor((Date.now() - date.getTime()) / 86400000));
}

function customerUncontactedDays(customer) {
  const explicit = Number(customer.uncontactedDays);
  if (Number.isFinite(explicit) && explicit >= 0) return Math.floor(explicit);
  const lastContact = parseCustomerDate(customer.lastContactAt || customer.lastContact || customer.createdAt || customer.created);
  return lastContact ? daysSince(lastContact) : Number.POSITIVE_INFINITY;
}

function customerUncontactedDaysLabel(customer) {
  const days = customerUncontactedDays(customer);
  return Number.isFinite(days) ? String(days) : "—";
}

function parseSelectedCustomerUncontactedDays(value) {
  const text = String(value ?? "").trim();
  if (!/^\d+$/.test(text)) return null;
  const days = Number(text);
  return Number.isSafeInteger(days) && days >= 0 ? days : null;
}

function selectedCustomerUncontactedDaysThreshold() {
  if (state.customerUncontactedDays === "all") return null;
  return parseSelectedCustomerUncontactedDays(state.customerUncontactedDays === "custom"
    ? state.customerUncontactedDaysCustom
    : state.customerUncontactedDays);
}

function parseSelectedCustomerAge(value) {
  const text = String(value ?? "").trim();
  if (!text) return null;
  if (!/^\d+$/.test(text)) return null;
  const age = Number(text);
  return Number.isSafeInteger(age) && age >= 0 && age <= 150 ? age : null;
}

function selectedCustomerAgeRange() {
  return {
    min: parseSelectedCustomerAge(state.customerAgeMin),
    max: parseSelectedCustomerAge(state.customerAgeMax)
  };
}

function customerMatchesAgeRange(customer, range) {
  if (range.min === null && range.max === null) return true;
  const age = parseSelectedCustomerAge(customer.age);
  if (age === null) return false;
  return (range.min === null || age >= range.min) && (range.max === null || age <= range.max);
}

function parseSelectedCustomerHeight(value) {
  const text = String(value ?? "").trim().replace(/\s*cm$/i, "");
  if (!text || !/^\d+$/.test(text)) return null;
  const height = Number(text);
  return Number.isSafeInteger(height) && height >= 0 && height <= 300 ? height : null;
}

function selectedCustomerHeightRange() {
  return {
    min: parseSelectedCustomerHeight(state.customerHeightMin),
    max: parseSelectedCustomerHeight(state.customerHeightMax)
  };
}

function customerMatchesHeightRange(customer, range) {
  if (range.min === null && range.max === null) return true;
  const height = parseSelectedCustomerHeight(customer.height);
  if (height === null) return false;
  return (range.min === null || height >= range.min) && (range.max === null || height <= range.max);
}

function selectedCustomerEducations(value = state.customerEducation) {
  if (Array.isArray(value)) return value.filter(item => item && item !== "all");
  return value === "all" || !value ? [] : [value];
}

function customerDateOnly(value) {
  const text = String(value || "").trim();
  const isoDate = text.match(/^\d{4}-\d{2}-\d{2}/);
  if (isoDate) return isoDate[0];
  const parsed = parseCustomerDate(text);
  return parsed ? calendarDateKey(parsed) : "";
}

function customerMatchesDateRangeValue(value, range = {}) {
  if (!range.start && !range.end) return true;
  const date = customerDateOnly(value);
  if (!date) return false;
  return (!range.start || date >= range.start) && (!range.end || date <= range.end);
}

function customerDateRangeMonthValue(value = "") {
  const text = String(value || "").slice(0, 7);
  return /^\d{4}-\d{2}$/.test(text) ? text : localDateValue(new Date()).slice(0, 7);
}

function customerDateRangeMonthDate(value) {
  const [year, month] = customerDateRangeMonthValue(value).split("-").map(Number);
  return new Date(year, month - 1, 1);
}

function customerDateRangeShiftMonth(value, offset) {
  const date = customerDateRangeMonthDate(value);
  date.setMonth(date.getMonth() + offset);
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}`;
}

function customerDateRangeMonthLabel(value) {
  const date = customerDateRangeMonthDate(value);
  return `${date.getFullYear()}年 ${date.getMonth() + 1}月`;
}

function customerDateRangeCalendar(monthValue, start, end, dateAttribute = "data-customer-date") {
  const cursor = customerDateRangeMonthDate(monthValue);
  const year = cursor.getFullYear();
  const month = cursor.getMonth();
  const firstDayOffset = (new Date(year, month, 1).getDay() + 6) % 7;
  const daysInMonth = new Date(year, month + 1, 0).getDate();
  const totalCells = Math.ceil((firstDayOffset + daysInMonth) / 7) * 7;
  const weekdays = ["一", "二", "三", "四", "五", "六", "日"];
  const cells = Array.from({length: totalCells}, (_, index) => {
    const date = new Date(year, month, index - firstDayOffset + 1);
    const key = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`;
    const inMonth = date.getMonth() === month;
    const inRange = start && end && key > start && key < end;
    const isStart = key === start;
    const isEnd = key === end;
    return `<button class="customer-date-range-day ${inMonth ? "" : "outside"} ${inRange ? "in-range" : ""} ${isStart ? "range-start" : ""} ${isEnd ? "range-end" : ""}" type="button" ${dateAttribute}="${key}" aria-label="${key}">${date.getDate()}</button>`;
  }).join("");
  return `<div class="customer-date-range-month"><strong>${customerDateRangeMonthLabel(monthValue)}</strong><div class="customer-date-range-weekdays">${weekdays.map(day => `<span>${day}</span>`).join("")}</div><div class="customer-date-range-days">${cells}</div></div>`;
}

function customerDateRangePickerView() {
  const start = String(state.customerDateRangeDraftStart || "");
  const end = String(state.customerDateRangeDraftEnd || "");
  const viewMonth = customerDateRangeMonthValue(state.customerDateRangeViewMonth || start);
  const nextMonth = customerDateRangeShiftMonth(viewMonth, 1);
  const selectedLabel = start && end
    ? `${start} 至 ${end}`
    : start
      ? `${start} 至 请选择结束日期`
      : "请选择开始日期和结束日期";
  return `<div class="date-range-popover open" id="dateRangePopover" role="dialog" aria-label="选择分配时间范围">
    <div class="date-range-popover-header"><strong>选择时间范围</strong><div class="date-range-popover-nav"><button type="button" data-customer-date-shift="-1" aria-label="上两个月">${icon("chevron-left")}</button><button type="button" data-customer-date-shift="1" aria-label="下两个月">${icon("chevron-right")}</button></div></div>
    <div class="customer-date-range-summary"><div class="${state.customerDateRangePicking === "start" ? "active" : ""}"><small>开始日期</small><strong>${start || "请选择"}</strong></div><span>→</span><div class="${state.customerDateRangePicking === "end" ? "active" : ""}"><small>结束日期</small><strong>${end || "请选择"}</strong></div></div>
    <div class="customer-date-range-calendars">${customerDateRangeCalendar(viewMonth, start, end)}${customerDateRangeCalendar(nextMonth, start, end)}</div>
    <div class="customer-date-range-footer"><span>${selectedLabel}</span><button type="button" data-clear-customer-date-range>清空</button></div>
  </div>`;
}

function customerAdvancedDateRangePickerView(field) {
  const start = String(state.customerAdvancedDatePickerDraftStart || "");
  const end = String(state.customerAdvancedDatePickerDraftEnd || "");
  const viewMonth = customerDateRangeMonthValue(state.customerAdvancedDatePickerViewMonth || start);
  const nextMonth = customerDateRangeShiftMonth(viewMonth, 1);
  const fieldLabels = { registration: "客户注册时间", lastLogin: "最近登录时间", firstAllocation: "首次分配时间", lastFollowUp: "最后跟进时间" };
  const selectedLabel = start && end
    ? `${start} 至 ${end}`
    : start
      ? `${start} 至 请选择结束日期`
      : "请选择开始日期和结束日期";
  return `<div class="date-range-popover open advanced-date-range-popover" id="customerAdvancedDateRangePicker" role="dialog" aria-label="选择${fieldLabels[field] || "时间范围"}">
    <div class="date-range-popover-header"><strong>选择${fieldLabels[field] || "时间范围"}</strong><div class="date-range-popover-nav"><button type="button" data-customer-advanced-date-shift="-1" aria-label="上两个月">${icon("chevron-left")}</button><button type="button" data-customer-advanced-date-shift="1" aria-label="下两个月">${icon("chevron-right")}</button></div></div>
    <div class="customer-date-range-summary"><div class="${state.customerAdvancedDatePickerPicking === "start" ? "active" : ""}"><small>开始日期</small><strong>${start || "请选择"}</strong></div><span>→</span><div class="${state.customerAdvancedDatePickerPicking === "end" ? "active" : ""}"><small>结束日期</small><strong>${end || "请选择"}</strong></div></div>
    <div class="customer-date-range-calendars">${customerDateRangeCalendar(viewMonth, start, end, "data-customer-advanced-date")}${customerDateRangeCalendar(nextMonth, start, end, "data-customer-advanced-date")}</div>
    <div class="customer-date-range-footer"><span>${selectedLabel}</span><button type="button" data-clear-customer-advanced-date-range>清空</button></div>
  </div>`;
}

function customerMatchesScene(customer, scene) {
  const today = localDateValue(new Date());
  const created = String(customer.createdAt || "").slice(0, 10);
  const nextFollow = String(customer.nextFollowAt || "").slice(0, 10);
  const followUps = customerFollowUpCount(customer);
  if (scene === "today-new") return created === today;
  if (scene === "today-follow") return nextFollow === today;
  if (scene === "new-unfollowed") return followUps === 0;
  if (scene === "two-days-unfollowed") return followUps === 0 && daysSince(customer.createdAt) >= 2;
  if (scene === "protected") return customer.level === "重点客户";
  if (scene === "duplicate-unfollowed") return Boolean(customer.duplicateRegistration) && followUps === 0;
  if (scene === "pool-claimed") return Boolean(customer.claimedFromPool);
  return true;
}

function customerSceneItems() {
  const source = customers.filter(customer => !["公海", "白板"].includes(customer.owner));
  return [
    ["all", "全部", false], ["today-new", "今日新分", true], ["today-follow", "今日待跟进", true],
    ["new-unfollowed", "新分未跟进", true], ["two-days-unfollowed", "两日未跟进", true],
    ["protected", "重点保护", false], ["duplicate-unfollowed", "重复注册未跟进", true],
    ["pool-claimed", "从公海捞取客户", false]
  ].map(([key, label, badge]) => ({ key, label, count: badge ? source.filter(customer => customerMatchesScene(customer, key)).length : null }));
}

function customerVisibleTableColumns() {
  const selected = new Set([...state.customerVisibleColumns, ...customerFixedColumnKeys]);
  return customerTableColumnDefinitions.filter(column => selected.has(column.key));
}

function customerSortNumber(value) {
  if (typeof value === "number") return Number.isFinite(value) ? value : null;
  const text = String(value ?? "").trim().replace(/,/g, "");
  const match = text.match(/-?\d+(?:\.\d+)?/);
  if (!match) return null;
  const number = Number(match[0]);
  if (!Number.isFinite(number)) return null;
  return /万/.test(text) ? number * 10000 : number;
}

function customerSortDate(value) {
  const date = parseCustomerDate(value);
  return date ? date.getTime() : null;
}

function customerSortValue(customer, key) {
  if (key === "age") return { type: "number", value: customerSortNumber(customer.age) };
  if (key === "income") return { type: "number", value: customerSortNumber(customer.monthlyIncome || customer.annualIncome || customer.amount) };
  if (key === "followUpCount") return { type: "number", value: customerFollowUpCount(customer) };
  if (key === "uncontactedDays") {
    const days = customerUncontactedDays(customer);
    return { type: "number", value: Number.isFinite(days) ? days : null };
  }
  if (key === "firstAllocationAt") return { type: "number", value: customerSortDate(customer.firstAllocationAt || customer.firstAssignedAt || customer.assignedAt || customer.lastAllocationAt) };
  if (key === "lastFollowUpAt") return { type: "number", value: customerSortDate(customer.lastContactAt || customer.lastContact || customer.updatedAt) };
  if (key === "lastLoginAt") return { type: "number", value: customerSortDate(customer.lastLoginAt || customer.lastLogin) };
  if (key === "education") {
    const text = String(customer.education ?? "").trim();
    const rank = customerEducationSortOrder.findIndex(item => text.includes(item));
    return { type: "number", value: rank < 0 ? null : rank };
  }
  return { type: "string", value: String(customer.city ?? "").trim() };
}

function compareCustomerSortValues(left, right, direction) {
  const leftMissing = left.value === null || left.value === "";
  const rightMissing = right.value === null || right.value === "";
  if (leftMissing || rightMissing) {
    if (leftMissing && rightMissing) return 0;
    return leftMissing ? 1 : -1;
  }
  const result = left.type === "number"
    ? left.value - right.value
    : left.value.localeCompare(right.value, "zh-CN", { numeric: true, sensitivity: "base" });
  return (direction === "desc" ? -1 : 1) * result;
}

function sortedCustomerRows(rows) {
  const { key, direction } = state.customerSort || {};
  if (!customerSortableColumnKeys.has(key) || !["asc", "desc"].includes(direction)) return rows;
  return rows
    .map((customer, index) => ({ customer, index }))
    .sort((left, right) => compareCustomerSortValues(
      customerSortValue(left.customer, key),
      customerSortValue(right.customer, key),
      direction
    ) || left.index - right.index)
    .map(item => item.customer);
}

function customerTableHeaderHtml(column) {
  const key = column.key || "";
  const sortable = customerSortableColumnKeys.has(key);
  const active = state.customerSort?.key === key && ["asc", "desc"].includes(state.customerSort.direction);
  const direction = active ? state.customerSort.direction : "";
  const className = [column.className || "", sortable ? "sortable-column" : ""].filter(Boolean).join(" ");
  const headerLabel = escapeHtml(column.label);
  const sortIndicator = `<span class="customer-sort-indicator ${direction}" aria-hidden="true"><span class="customer-sort-arrow customer-sort-arrow-up" data-customer-sort-direction="asc" title="升序"></span><span class="customer-sort-arrow customer-sort-arrow-down" data-customer-sort-direction="desc" title="降序"></span></span>`;
  const content = sortable
    ? `<button class="customer-sort-trigger" type="button" data-customer-sort="${escapeHtml(key)}" aria-label="按${headerLabel}${direction === "asc" ? "降序" : "升序"}" title="点击切换${headerLabel}排序">${headerLabel}${sortIndicator}</button>`
    : headerLabel;
  return `<th${className ? ` class="${className}"` : ""}${key ? ` data-customer-column="${escapeHtml(key)}"` : ""}${active ? ` aria-sort="${direction === "asc" ? "ascending" : "descending"}` : ""}>${content}</th>`;
}

function customerTableDisplayValue(value) {
  return value === null || value === undefined || value === "" ? "—" : String(value);
}

function customerOwnerDisplay(owner) {
  return owner === "白板" || !String(owner || "").trim() ? "未分配" : String(owner);
}

function customerTableColumnHtml(customer, key) {
  if (key === "tag") return `<span class="customer-table-flag">⚑</span>`;
  if (key === "id") return `<a class="table-link" onclick="event.stopPropagation()">${escapeHtml(customer.id)}</a>`;
  if (key === "customer") {
    return `<div class="customer-cell"><span class="person-avatar">${escapeHtml(customer.name?.[0] || "客")}</span><span><strong>${escapeHtml(customer.name || "—")}</strong><small>${escapeHtml(customer.phone || "")}</small></span></div>`;
  }
  if (key === "income") return escapeHtml(customerTableDisplayValue(customer.monthlyIncome || customer.annualIncome || customer.amount));
  if (key === "followUpCount") return escapeHtml(String(customerFollowUpCount(customer)));
  if (key === "uncontactedDays") return escapeHtml(customerUncontactedDaysLabel(customer));
  if (key === "customerStatus") return escapeHtml(customerTableDisplayValue(customer.statusCategory || customer.customerStatus || customer.stage));
  if (key === "firstAllocationAt") return escapeHtml(formatDateTime(customer.firstAllocationAt || customer.firstAssignedAt || customer.assignedAt || customer.lastAllocationAt));
  if (key === "lastFollowUpAt") return escapeHtml(formatDateTime(customer.lastContactAt || customer.lastContact || customer.updatedAt));
  if (key === "lastLoginAt") return escapeHtml(formatDateTime(customer.lastLoginAt || customer.lastLogin));
  if (key === "tags") return escapeHtml(customerTableDisplayValue((customer.tags || []).join("、")));
  const values = {
    gender: customer.gender,
    maritalStatus: customer.maritalStatus,
    age: customer.age,
    education: customer.education,
    level: customer.level,
    city: customer.city,
    source: customer.source,
    owner: customerOwnerDisplay(customer.owner),
    inviter: customer.inviter,
    collaborator: customer.collaborator,
    servicePerson: customer.servicePerson || customer.serviceOwner
  };
  return escapeHtml(customerTableDisplayValue(values[key]));
}

function customerHeaderModalView() {
  const fixed = new Set(customerFixedColumnKeys);
  const selected = new Set([...state.customerHeaderDraftColumns, ...customerFixedColumnKeys]);
  const configurableColumns = customerTableColumnDefinitions.filter(column => !fixed.has(column.key));
  const selectedColumns = customerTableColumnDefinitions.filter(column => selected.has(column.key));
  return `<div class="modal-backdrop customer-header-backdrop" role="presentation"><section class="customer-header-modal" role="dialog" aria-modal="true" aria-label="字段设置">
    <header><h2>字段设置</h2><button type="button" id="closeCustomerHeader" aria-label="关闭">×</button></header>
    <div class="customer-header-body"><section class="customer-header-available"><div class="customer-header-section-head"><h3>基本信息</h3><label><input type="checkbox" id="selectAllCustomerColumns" ${configurableColumns.every(column => selected.has(column.key)) ? "checked" : ""}>全选</label></div><div class="customer-header-options">${customerTableColumnDefinitions.map(column => `<label class="${fixed.has(column.key) ? "is-fixed" : ""}"><input type="checkbox" data-customer-header-column="${column.key}" ${selected.has(column.key) ? "checked" : ""} ${fixed.has(column.key) ? "disabled" : ""}>${escapeHtml(column.label)}</label>`).join("")}</div></section><aside class="customer-header-selected"><h3>当前选择字段</h3><ol>${selectedColumns.map(column => `<li><span class="customer-header-drag-handle" aria-hidden="true">⋮⋮</span><span>${escapeHtml(column.label)}</span></li>`).join("")}</ol></aside></div>
    <footer><span></span><button class="button secondary" type="button" id="cancelCustomerHeader">取消</button><button class="button primary" type="button" id="saveCustomerHeader">确定</button></footer>
  </section></div>`;
}

function customerListView({ isPool = false } = {}) {
  const rows = sortedCustomerRows(isPool ? publicPoolCustomers() : filteredCustomers());
  const totalPages = Math.max(1, Math.ceil(rows.length / state.customerPageSize));
  state.customerPage = Math.min(state.customerPage, totalPages);
  const pageStart = (state.customerPage - 1) * state.customerPageSize;
  const pageRows = rows.slice(pageStart, pageStart + state.customerPageSize);
  return `<section class="page customer-list-page ${isPool ? "pool-page" : ""}">
    ${subnav(["客户列表", "公海列表", "客户导入"], state.customerSection, ["客户列表", "公海列表", "客户导入"])}
    <div class="page-content">
      ${isPool ? "" : `<div class="customer-reference-tabs">${[["all","全部客户"],["mine","我的客户"],["subordinates","下属客户"],["collab","我的协作"],["subordinate-collab","下属协作"],["store","到店客户"]].map(([scope, label]) => `<button class="${state.customerScope === scope ? "active" : ""}" type="button" data-customer-scope="${scope}">${label}</button>`).join("")}</div>`}
      <div class="customer-scene-bar"><span>场景：</span>${customerSceneItems().map(item => `<button class="scene-chip ${state.customerScene === item.key ? "active" : ""}" type="button" data-customer-scene="${item.key}">${item.label}${item.count > 0 ? `<b>${item.count}</b>` : ""}</button>`).join("")}<span class="pool-label">标注/标签：</span><button class="tag-filter" type="button"><span class="flag-icon no-tag-icon">⚑</span> 无标签</button><button class="tag-filter important-tag" type="button"><span class="flag-icon">⚑</span> 重点客户</button><button class="tag-filter" type="button">⚑ 普通客户</button><button class="text-button" type="button">更多</button></div>
      <section class="filter-panel">
        <div class="filter-row">
          <label class="field"><span>筛选条件</span><input id="customerSearch" type="search" value="${state.customerSearch}" placeholder="ID/手机号"></label>
          <label class="field"><span>&nbsp;</span><input id="customerNameSearch" type="search" value="${escapeHtml(state.customerNameSearch)}" placeholder="姓名/昵称/备注"></label>
          <label class="field"><span>&nbsp;</span>${isPool ? `<select><option>深沟时长</option></select>` : `<button id="stageFilter" class="filter-trigger" type="button">全部</button>`}</label>
          ${isPool ? `<label class="field pool-follow-range"><span>&nbsp;</span><div><input type="search" placeholder="最小跟进次数"><input type="search" placeholder="最大跟进次数"></div></label>` : `<label class="field status-filter-field"><span>&nbsp;</span><div class="status-filter"><input id="levelFilter" type="search" value="${state.customerStatus === "全部状态" ? "" : state.customerStatus}" placeholder="请选择客户状态" autocomplete="off"><div class="status-filter-menu">${customerStatusOptions.map(option => `<button type="button" data-customer-status="${option}">${option}</button>`).join("")}</div></div></label><label class="field"><span>&nbsp;</span>${customerOwnerCascadeControl("main", state.customerOwnerSelection, state.customerOwnerCascadeOpen)}</label>`}<label class="field customer-date-range"><span>&nbsp;</span><div class="date-range-picker-wrap"><div class="date-range-display"><span>${isPool ? "入海开始时间" : state.customerStartDate || "分配开始时间"}</span><b>→</b><span>${isPool ? "入海结束时间" : state.customerEndDate || "分配结束时间"}</span><button class="date-range-picker" id="openDateRangePicker" type="button" aria-label="选择分配时间范围">${icon("calendar")}</button></div>${isPool ? "" : (state.customerDateRangePickerOpen ? customerDateRangePickerView() : "")}</div></label><div class="inline-actions customer-filter-actions"><button class="button primary" id="applyCustomerFilters" type="button">${icon("search")}查询</button><button class="button secondary" id="resetCustomerFilters" type="button">重置</button><button class="text-button" type="button">高级筛选</button></div>
        </div>
        <div class="filter-footer"><div class="filter-tags">${["全部客户","重点客户","今日待跟进","即将成交"].map(item => `<button class="quick-filter ${state.quickFilter === item ? "active" : ""}" type="button" data-quick-filter="${item}">${item}</button>`).join("")}</div></div>
      </section>
      <div class="customer-batch-actions"><label class="wechat-toggle"><input type="checkbox"> 添加微信客户</label><div class="batch-action-row">${!isPool && isAdmin() ? `<button class="button primary" type="button" id="customerAllocate">${icon("sliders")}资源调配</button>` : ""}${isPool ? `<button class="button secondary" id="batchClaimCustomers" type="button">${icon("users")}领取客户</button>` : `<button class="button primary" type="button" data-add-customer>${icon("plus")}增加用户</button><button class="button secondary" id="batchPoolCustomers" type="button">${icon("user-transfer")}移入公海</button>`}${isPool ? "" : `<button class="button secondary" id="batchMessageCustomers" type="button">${icon("chat-circle")}发送消息</button>`}<button class="button secondary" type="button">${icon("user-transfer")}转为库存</button>${isPool ? "" : `<button class="button secondary" type="button" id="openCustomerHeader">${icon("table-grid")}自定义表头</button>`}</div><div class="customer-selection-summary">已选择 <strong>${state.selectedCustomerIds.length}</strong> 项${state.selectedCustomerIds.length ? `　<button class="text-button" id="clearCustomerSelection">清空</button>` : ""}</div>${isPool ? "" : `<div class="customer-id-hints"><span class="unregistered-id">*橙色ID为未注册用户</span><span class="unavailable-id">灰色ID为当前不可拨打用户</span></div>`}</div>
      <section class="data-panel">
        <div class="data-toolbar"><div class="data-tabs"><button class="data-tab ${isPool || state.customerScope === "all" ? "active" : ""}" type="button" ${isPool ? "" : `data-customer-scope="all"`}>全部 <span class="count">${rows.length}</span></button>${isPool ? "" : `<button class="data-tab ${state.customerScope === "mine" ? "active" : ""}" type="button" data-customer-scope="mine">我负责的</button><button class="data-tab" type="button" disabled>我协作的</button>`}</div><div class="toolbar-actions"><span class="pill gray">当前显示 ${rows.length} 项</span></div></div>
      <div class="table-wrap">${rows.length ? `<table class="data-table customer-detail-table"><thead><tr><th class="select-column"><input id="selectPageCustomers" type="checkbox" aria-label="选择本页客户" ${pageRows.length && pageRows.every(c => state.selectedCustomerIds.includes(c.id)) ? "checked" : ""}></th><th>标注</th><th>ID</th><th>客户姓名/昵称</th><th>性别</th><th>婚况</th><th>年龄</th><th>学历</th><th>收入</th><th>等级</th><th>城市</th><th>跟进次数</th><th>未联系天数</th><th>${isPool ? "前归属人" : "归属人"}</th><th>邀约人</th><th>协作人</th><th>服务人</th><th class="operation-column">操作</th></tr></thead><tbody>${pageRows.map(customer => `<tr data-customer-id="${escapeHtml(customer.id)}"><td class="select-column"><input type="checkbox" data-select-customer="${escapeHtml(customer.id)}" aria-label="选择${escapeHtml(customer.name)}" ${state.selectedCustomerIds.includes(customer.id) ? "checked" : ""} onclick="event.stopPropagation()"></td><td>⚑</td><td><a class="table-link" onclick="event.stopPropagation()">${escapeHtml(customer.id)}</a></td><td><div class="customer-cell"><span class="person-avatar">${escapeHtml(customer.name[0])}</span><span><strong>${escapeHtml(customer.name)}</strong><small>${escapeHtml(customer.phone)}</small></span></div></td><td>${escapeHtml(customer.gender || "—")}</td><td>${escapeHtml(customer.maritalStatus || "—")}</td><td>${escapeHtml(customer.age || "—")}</td><td>${escapeHtml(customer.education || "—")}</td><td>${escapeHtml(customer.monthlyIncome || customer.annualIncome || "—")}</td><td>${escapeHtml(customer.level || "—")}</td><td>${escapeHtml(customer.city || "—")}</td><td>${escapeHtml(String(customer.followUpCount || 0))}</td><td>${escapeHtml(customerUncontactedDaysLabel(customer))}</td><td>${escapeHtml(isPool ? customerOwnerDisplay(customer.previousOwner) : customerOwnerDisplay(customer.owner))}</td><td>${escapeHtml(customer.inviter || "—")}</td><td>${escapeHtml(customer.collaborator || "—")}</td><td>${escapeHtml(customer.servicePerson || "—")}</td><td class="operation-column"><div class="table-actions" onclick="event.stopPropagation()"><button class="table-icon" type="button" data-call="${escapeHtml(customer.id)}" aria-label="呼叫客户">${icon("phone")}</button><button class="table-icon" type="button" data-open-customer="${escapeHtml(customer.id)}" aria-label="查看详情">${icon("chevron")}</button><button class="button ghost" type="button" ${isPool ? `data-claim-customer="${escapeHtml(customer.id)}"` : `data-release-customer="${escapeHtml(customer.id)}"`}>${isPool ? "领取" : "放入公海"}</button></div></td></tr>`).join("")}</tbody></table>` : `<div class="empty-state"><span class="empty-icon">${icon(isPool ? "users" : "search")}</span><strong>${isPool ? "公海暂无客户" : "没有匹配的客户"}</strong><p>${isPool ? "可以从客户列表将客户放入公海。" : "调整筛选条件后再试一次"}</p></div>`}</div>
        <footer class="pagination"><span>${rows.length ? `${pageStart + 1}-${Math.min(pageStart + state.customerPageSize, rows.length)}` : "0"} 共 ${rows.length} 条</span><button class="page-button" data-customer-page="${state.customerPage - 1}" type="button" ${state.customerPage <= 1 ? "disabled" : ""}>‹</button>${Array.from({length: Math.min(totalPages, 5)}, (_, i) => i + 1).map(page => `<button class="page-button ${page === state.customerPage ? "active" : ""}" data-customer-page="${page}" type="button">${page}</button>`).join("")}<button class="page-button" data-customer-page="${state.customerPage + 1}" type="button" ${state.customerPage >= totalPages ? "disabled" : ""}>›</button><select id="customerPageSize"><option ${state.customerPageSize===20?"selected":""}>20</option><option ${state.customerPageSize===50?"selected":""}>50</option><option ${state.customerPageSize===100?"selected":""}>100</option></select><span>条/页</span></footer>
      </section>
    </div>
  </section>${!isPool && state.customerAdvancedOpen ? whiteboardAdvancedFilterView() : ""}${!isPool && state.customerHeaderModalOpen ? customerHeaderModalView() : ""}`;
}

function publicPoolCustomers() {
  const byId = new Map();
  [...customers, ...state.poolCustomers].forEach(customer => {
    if (customer.owner === "公海") byId.set(customer.id, customer);
  });
  return [...byId.values()];
}

function customerPoolView() {
  return customerListView({ isPool: true });
}


const customerImportDetailColumns = [
  { key: "name", label: "姓名", required: true },
  { key: "phone", label: "电话号码", required: true },
  { key: "gender", label: "性别" },
  { key: "birthday", label: "生日" },
  { key: "age", label: "年龄" },
  { key: "height", label: "身高" },
  { key: "maritalStatus", label: "婚况" },
  { key: "education", label: "学历" },
  { key: "monthlyIncome", label: "月收入" },
  { key: "occupation", label: "职业" },
  { key: "housing", label: "住房" },
  { key: "car", label: "购车" },
  { key: "nativePlace", label: "籍贯" },
  { key: "workLocation", label: "工作地" }
];

function importHistoryRecord(id) {
  return state.importHistory.find(record => String(record.id) === String(id));
}

function persistImportHistory() {
  try {
    localStorage.setItem("youke.crm.importHistory", JSON.stringify(state.importHistory));
  } catch (_) {
    // A large spreadsheet can exceed localStorage; the active detail still works in memory.
  }
}

function importRowsForRecord(record) {
  return Array.isArray(record?.rows) ? record.rows : [];
}

function importRowValidation(row, rows) {
  const errors = [];
  const name = String(row?.name || "").trim();
  const phone = normalizeImportPhone(row?.phone);
  if (!name) errors.push("姓名不能为空");
  if (!/^1[3-9]\d{9}$/.test(phone)) errors.push("手机号异常");
  if (phone) {
    const duplicateInFile = rows.findIndex(candidate => normalizeImportPhone(candidate.phone) === phone);
    if (duplicateInFile >= 0 && rows[duplicateInFile] !== row) errors.push("文件内手机号重复");
    const duplicateInSystem = [...customers, ...(state.poolCustomers || [])].some(customer => normalizeImportPhone(customer.phone) === phone);
    if (duplicateInSystem) errors.push("客户已存在");
  }
  return { errors, phone, requiredInvalid: !name || !/^1[3-9]\d{9}$/.test(phone) };
}

function importRowDisplayValue(row, key) {
  const value = String(row?.[key] ?? "").trim();
  return value || "—";
}

function importRowStatus(row, validation) {
  if (row?._importStatus === "已导入") return { label: "已导入", className: "success" };
  if (row?._importStatus === "已跳过") return { label: "已跳过", className: "skipped" };
  if (row?._importStatus === "导入失败") return { label: "导入失败", className: "failed" };
  return { label: "未导入", className: validation.errors.length ? "warning" : "pending" };
}

function customerImportDetailView(record) {
  const rows = importRowsForRecord(record);
  const filteredRows = rows.map((row, index) => ({ row, index })).filter(({ row }) => {
    return state.importDetailStatusFilter === "全部状态"
      || (state.importDetailStatusFilter === "未导入" && (!row._importStatus || row._importStatus === "未导入"))
      || row._importStatus === state.importDetailStatusFilter;
  });
  const pendingRows = rows.map((row, index) => ({ row, index })).filter(({ row }) => !["已导入", "已跳过"].includes(row._importStatus));
  const selectedRows = state.importSelectedRows.filter(index => pendingRows.some(item => item.index === index));
  const processed = rows.filter(row => ["已导入", "已跳过"].includes(row._importStatus)).length;
  const failed = rows.filter(row => row._importStatus === "导入失败").length;
  const allPendingSelected = pendingRows.length > 0 && pendingRows.every(({ index }) => selectedRows.includes(index));
  const canImport = pendingRows.length > 0 && state.importEditingRow === null;
  const recordName = record?.fileName || `批次 ${record?.id || ""}`;
  return `<section class="page customer-import-detail-page">
    ${subnav(["首页", "客户导入"], "客户导入", ["首页", "客户导入"])}
    <div class="page-content">
      <section class="customer-import-detail-header"><div><h1>导入详情</h1><p><span class="import-batch-dot"></span>${escapeHtml(recordName)} <span class="import-header-meta">上传于 ${escapeHtml(record?.uploadedAt || "—")} · 共 ${rows.length} 条</span></p></div><div class="import-detail-summary"><strong>${processed}</strong><span>/ ${rows.length} 已处理</span>${failed ? `<em>${failed} 条失败待修改</em>` : ""}</div></section>
      <section class="customer-import-detail-toolbar"><div class="customer-import-detail-filter"><label><strong>导入状态：</strong><select id="importDetailStatusFilter">${["全部状态", "未导入", "已导入", "已跳过", "导入失败"].map(status => `<option ${state.importDetailStatusFilter === status ? "selected" : ""}>${status}</option>`).join("")}</select></label><button class="button primary" id="applyImportDetailFilters" type="button">查询</button><button class="button secondary" id="resetImportDetailFilters" type="button">重置</button></div><div class="customer-import-detail-actions"><button class="button secondary" type="button" data-import-selected ${selectedRows.length && canImport ? "" : "disabled"}>导入选中${selectedRows.length ? ` (${selectedRows.length})` : ""}</button><button class="button primary" type="button" data-import-all ${canImport ? "" : "disabled"}>${icon("download")}全部导入</button><label class="import-skip-toggle"><input id="importSkipInvalid" type="checkbox" ${state.importSkipInvalid ? "checked" : ""}><i></i><span>跳过电话号码异常及重复记录</span></label></div></section>
      <section class="customer-import-detail-table-panel"><div class="customer-import-detail-table-wrap"><table class="data-table customer-import-detail-table"><thead><tr><th class="import-select-column"><input id="selectImportRows" type="checkbox" aria-label="选择待导入客户" ${allPendingSelected ? "checked" : ""}></th><th class="import-status-column">导入状态</th>${customerImportDetailColumns.map(column => `<th class="${column.required ? "required-column" : ""}">${escapeHtml(column.label)}${column.required ? "*" : ""}</th>`).join("")}<th class="import-operation-column">操作</th></tr></thead><tbody>${filteredRows.length ? filteredRows.map(({ row, index }) => {
    const validation = importRowValidation(row, rows);
    const status = importRowStatus(row, validation);
    const editing = state.importEditingRow === index;
    const editable = !["已导入", "已跳过"].includes(row._importStatus);
    const rowClass = validation.errors.length ? "has-import-warning" : "";
    return `<tr class="${rowClass}" data-import-row-index="${index}"><td class="import-select-column"><input type="checkbox" data-import-select="${index}" aria-label="选择${escapeHtml(row.name || `第${index + 1}条`)}" ${selectedRows.includes(index) ? "checked" : ""} ${editable ? "" : "disabled"}></td><td class="import-status-column"><span class="import-row-status ${status.className}">${status.label}</span>${validation.errors.length ? `<small title="${escapeHtml(validation.errors.join("、"))}">${escapeHtml(validation.errors.join("、"))}</small>` : row._importMessage ? `<small>${escapeHtml(row._importMessage)}</small>` : ""}</td>${customerImportDetailColumns.map(column => `<td class="import-field-${column.key}">${editing ? `<input type="${column.key === "birthday" ? "date" : "text"}" data-import-field="${column.key}" value="${escapeHtml(String(row[column.key] ?? ""))}" ${column.required ? "required" : ""}>` : `<span>${escapeHtml(importRowDisplayValue(row, column.key))}</span>`}</td>`).join("")}<td class="import-operation-column"><div class="import-row-actions">${editing ? `<button class="table-link" type="button" data-import-save="${index}">确定</button><button class="table-link muted-link" type="button" data-import-cancel="${index}">取消</button>` : editable ? `<button class="table-link" type="button" data-import-edit="${index}">修改</button><button class="table-link" type="button" data-import-row="${index}">导入</button><button class="table-link danger-link" type="button" data-import-delete="${index}">删除</button>` : `<span class="import-complete-mark">${status.label}</span>`}</div></td></tr>`;
  }).join("") : `<tr><td colspan="${customerImportDetailColumns.length + 3}"><div class="customer-import-empty"><span class="empty-icon">${icon("users")}</span><strong>${rows.length ? "没有符合当前状态的记录" : "暂无客户明细"}</strong>${rows.length ? `<p>点击“重置”查看全部导入明细。</p>` : ""}</div></td></tr>`}</tbody></table></div><footer class="pagination customer-import-detail-pagination"><span>${filteredRows.length ? `显示 ${filteredRows.length} 条` : "0 条"}，共 ${rows.length} 条</span><span>已处理 ${processed} 条${failed ? `，失败 ${failed} 条` : ""}</span></footer></section>
    </div>
  </section>`;
}

function customerImportView() {
  const detailRecord = state.importDetailId ? importHistoryRecord(state.importDetailId) : null;
  if (detailRecord) return customerImportDetailView(detailRecord);
  const records = state.importHistory.filter(record => state.importStatusFilter === "全部状态" || record.status === state.importStatusFilter);
  const progress = record => record.status === "已完成" ? 100 : record.status === "上传失败" ? 0 : Math.round(((record.success || 0) + (record.fail || 0) + (record.skipped || 0)) / Math.max(1, record.count) * 100);
  return `<section class="page customer-import-page">
    ${subnav(["首页", "客户导入"], "客户导入", ["首页", "客户导入"])}
    <div class="page-content">
      <section class="customer-import-filter"><div class="customer-import-filter-row"><label><strong>上传时间：</strong><div><input type="date" aria-label="上传开始日期"><b>→</b><input type="date" aria-label="上传结束日期"></div></label><label><strong>上传状态：</strong><select id="importStatusFilter"><option>全部状态</option>${["待导入", "已完成", "上传失败"].map(status => `<option ${state.importStatusFilter === status ? "selected" : ""}>${status}</option>`).join("")}</select></label><button class="button primary" id="applyImportFilters" type="button">查询</button><button class="button secondary" id="resetImportFilters" type="button">重置</button><div class="customer-import-upload-actions"><button class="button primary" id="downloadCustomerTemplate" type="button">${icon("download")}下载模板</button><label class="button primary" for="customerImportFile">${icon("download")}数据上传<input id="customerImportFile" type="file" accept=".csv,text/csv" hidden></label><small>只允许上传50M以下的文件</small></div></div></section>
      <p class="customer-import-routing-note">导入成功的客户会自动进入白板列表，由管理员统一分配给销售员工；上传后可进入“导入详情”修改客户信息，再单条或批量确认导入。</p>
      <section class="customer-import-table-panel"><div class="table-wrap"><table class="data-table customer-import-table"><thead><tr><th>上传时间</th><th>上传状态</th><th>上传数量</th><th>导入进度</th><th>上传人</th><th>操作</th></tr></thead><tbody>${records.length ? records.map(record => { const value = progress(record); return `<tr><td>${escapeHtml(record.uploadedAt)}</td><td><div class="import-status-progress ${record.status === "上传失败" ? "failed" : ""}"><i><span style="width:${value}%"></span></i><strong>${escapeHtml(record.status)}</strong>${record.message ? `<small>${escapeHtml(record.message)}</small>` : ""}</div></td><td>${record.count || 0}</td><td><div class="import-execution"><i><span style="width:${value}%"></span></i><strong>${(record.success || 0) + (record.skipped || 0)}/${record.count || 0}${record.status === "待导入" ? "待导入" : "执行完成"}</strong><div><em>成功 ${record.success || 0} 条</em><b>失败 ${record.fail || 0} 条${record.skipped ? ` · 跳过 ${record.skipped} 条` : ""}</b></div></div></td><td>${escapeHtml(record.uploader || "—")}</td><td><button class="table-link" type="button" data-import-detail="${record.id}">导入详情</button></td></tr>`; }).join("") : `<tr><td colspan="6"><div class="customer-import-empty"><span class="empty-icon">${icon("download")}</span><strong>暂无上传记录</strong><p>点击右上角“数据上传”选择客户 CSV 或 Excel 文件</p></div></td></tr>`}</tbody></table></div><footer class="pagination"><span>共 ${records.length} 条</span><button class="page-button" disabled>‹</button><button class="page-button active">1</button><button class="page-button" disabled>›</button><span>20 条/页</span></footer></section>
    </div>
  </section>`;
}

function customerTagView() {
  const records = [...customers, ...state.poolCustomers].filter((item, index, all) => all.findIndex(candidate => candidate.id === item.id) === index && (isAdmin() || item.owner === currentOwner()));
  const tagMap = new Map();
  records.forEach(customer => (customer.tags || []).forEach(tag => tagMap.set(tag, (tagMap.get(tag) || 0) + 1)));
  const tags = [...tagMap.entries()].sort((a, b) => a[0].localeCompare(b[0], "zh-CN"));
  return `<section class="page">
    ${subnav(["客户列表", "公海列表", "客户导入"], state.customerSection, ["客户列表", "公海列表", "客户导入"])}
    <div class="page-content">
      ${pageHeading(`<span class="pill gray">${tags.length} 个标签</span>`)}
      <section class="data-panel"><div class="data-toolbar"><div class="panel-title"><h2>新增标签</h2><span>添加后可在客户资料中继续使用</span></div></div><form class="tag-create-form" id="tagCreateForm"><input name="tag" required maxlength="32" placeholder="例如：高意向"><select name="customerId" required>${records.map(customer => `<option value="${escapeHtml(customer.id)}">${escapeHtml(customer.name)} · ${escapeHtml(customer.company)}</option>`).join("")}</select><button class="button primary" type="submit">${icon("plus")}添加到客户</button></form></section>
      <section class="data-panel"><div class="data-toolbar"><div class="panel-title"><h2>标签列表</h2><span>重命名和删除会同步更新客户资料</span></div></div><div class="tag-manager-list">${tags.length ? tags.map(([tag, count]) => `<div class="tag-manager-row"><span class="tag">${escapeHtml(tag)}</span><span class="tag-count">${count} 个客户</span><span class="modal-footer-spacer"></span><button class="button ghost" type="button" data-rename-tag="${escapeHtml(tag)}">重命名</button><button class="button danger" type="button" data-delete-tag="${escapeHtml(tag)}">删除</button></div>`).join("") : `<div class="empty-state"><span class="empty-icon">${icon("users")}</span><strong>还没有标签</strong><p>先选择一个客户添加标签。</p></div>`}</div></section>
    </div>
  </section>`;
}

function sincereResourceView() {
  const query = state.customerSearch.trim().toLowerCase();
  const rows = customers.filter(customer => customer.owner === "公海" || customer.owner === "鍏捣").filter(customer => {
    const matchesQuery = !query || [customer.id, customer.name, customer.phone, customer.company].join(" ").toLowerCase().includes(query);
    const matchesOwner = state.customerOwner === "全部负责人" || customer.owner === state.customerOwner;
    const matchesScope = state.sincereScope === "all"
      || (state.sincereScope === "mine" && customer.owner === currentOwner())
      || (state.sincereScope === "subordinates" && customer.owner !== currentOwner());
    return customer.owner !== "公海" && matchesQuery && matchesOwner && matchesScope;
  });
  const card = (customer, index) => {
    const gender = customer.gender || (index % 4 === 3 ? "男" : "女");
    const age = customer.age || `${30 + (index % 7)}岁`;
    const education = customer.education || "本科";
    const maskedPhone = String(customer.phone || "").replace(/^(\d{3})\d{4}(\d{4})$/, "$1****$2");
    return `<article class="sincere-card" data-resource-customer="${escapeHtml(customer.id)}" tabindex="0">
      <div class="sincere-avatar sincere-avatar-${gender === "男" ? "male" : "female"}"><span>${escapeHtml(customer.name[0])}</span><i></i></div>
      <div class="sincere-card-body"><div class="sincere-card-title"><button type="button" data-open-customer="${escapeHtml(customer.id)}">ID:${escapeHtml(customer.id)}</button><strong>${escapeHtml(customer.name || maskedPhone)}</strong></div>
      <div class="sincere-card-meta"><span>${escapeHtml(gender)}</span><span>${escapeHtml(age)}</span><span>${escapeHtml(education)}</span><span>${escapeHtml(customer.city || "地区待完善")}</span></div>
      <div class="sincere-card-footer"><span>${escapeHtml(customer.level || "普通客户")}</span><span>操作人：${escapeHtml(customer.owner || "—")}</span></div></div>
    </article>`;
  };
  return `<section class="page sincere-resource-page">
    ${subnav(["客户列表", "公海列表", "诚意资源", "客户导入"], state.customerSection, ["客户列表", "公海列表", "诚意资源", "客户导入"])}
    <div class="page-content">
      <div class="sincere-scope-tabs">${[["all", "全部资源"], ["mine", "我的资源"], ["subordinates", "下属资源"]].map(([scope, label]) => `<button class="${state.sincereScope === scope ? "active" : ""}" type="button" data-sincere-scope="${scope}">${label}</button>`).join("")}</div>
      <section class="sincere-filter-panel">
        <div class="sincere-filter-grid">
          <label><span>昵称</span><input id="customerSearch" value="${escapeHtml(state.customerSearch)}" placeholder="请输入姓名/昵称"></label>
          <label><span>性别</span><select><option>请选择</option><option>女</option><option>男</option></select></label>
          <label class="range-field"><span>年龄</span><div><input type="number" placeholder="岁"><b>–</b><input type="number" placeholder="岁"></div></label>
          <label><span>婚况</span><select><option>请选择</option><option>未婚</option><option>离异</option><option>丧偶</option></select></label>
          <label class="range-field"><span>身高</span><div><input type="number" placeholder="cm"><b>–</b><input type="number" placeholder="cm"></div></label>
          <label><span>学历</span><select><option>请选择</option><option>本科</option><option>硕士</option><option>大专</option></select></label>
          <label class="range-field"><span>收入</span><div><input type="number" placeholder="元"><b>–</b><input type="number" placeholder="元"></div></label>
          <label class="range-field"><span>入库时间</span><div><input type="date"><b>–</b><input type="date"></div></label>
          <label><span>操作人</span><select id="ownerFilter"><option>全部负责人</option>${ownerOptions(state.customerOwner)}</select></label>
          <label><span>ID/手机号</span><input id="sincereIdSearch" value="${escapeHtml(state.customerSearch)}" placeholder="请输入资源ID或手机号"></label>
          <label><span>有无头像</span><select><option>请选择</option><option>有头像</option><option>无头像</option></select></label>
        </div>
        <div class="sincere-advanced"><span>高级筛选</span><div></div></div>
        <div class="sincere-filter-actions"><button class="button secondary" type="button">保存为模板</button><button class="button primary" id="applyCustomerFilters" type="button">查询</button><button class="button secondary" id="resetCustomerFilters" type="button">重置</button><button class="text-button" type="button">${icon("sliders")} 高级筛选</button></div>
      </section>
      <div class="sincere-resource-toolbar"><button class="button primary" type="button" data-add-customer>${icon("plus")}增加用户</button><span>共 ${rows.length} 条诚意资源</span></div>
      ${rows.length
        ? `<section class="sincere-card-grid">${rows.map(card).join("")}</section>`
        : `<section class="sincere-no-data"><div class="sincere-empty-illustration"><span></span><i></i><b>•••</b></div><strong>暂无数据</strong><footer><span>0-0 共0条</span><button type="button" disabled>‹</button><button type="button" class="active">1</button><button type="button" disabled>›</button><select aria-label="每页条数"><option>8 条/页</option></select></footer></section>`}
    </div>
  </section>`;
}

function whiteboardListView() {
  const query = state.customerSearch.trim().toLowerCase();
  const nameQuery = state.whiteboardNameSearch.trim().toLowerCase();
  const rows = filteredCustomers({ includeWhiteboard: true }).filter(customer => {
    const matchesQuery = !query || [customer.id, customer.phone, customer.name, customer.company].join(" ").toLowerCase().includes(query);
    const matchesName = !nameQuery || [customer.name, customer.company, customer.remark, customer.notes, customer.nickname].join(" ").toLowerCase().includes(nameQuery);
    const matchesStatus = state.whiteboardStatus === "全部" || (customer.stage || "未激活") === state.whiteboardStatus;
    const matchesTags = !state.whiteboardSelectedTags.length || state.whiteboardSelectedTags.some(tag => (customer.tags || []).includes(tag));
    return customer.owner === "白板" && matchesQuery && matchesName && matchesStatus && matchesTags;
  });
  const canAllocate = isAdmin();
  return `<section class="page whiteboard-page">
    ${subnav(["客户列表", "公海列表", "诚意资源", "白板列表", "客户导入"], state.customerSection, ["客户列表", "公海列表", "诚意资源", "白板列表", "客户导入"])}
    <div class="page-content">
      <section class="whiteboard-filter-panel">
        <div class="whiteboard-filter-row"><span>筛选条件：</span><input id="customerSearch" value="${escapeHtml(state.customerSearch)}" placeholder="ID/手机号"><input placeholder="洗白ID"><input id="whiteboardNameSearch" value="${escapeHtml(state.whiteboardNameSearch)}" placeholder="姓名/昵称/备注"><select id="whiteboardStatusFilter"><option ${state.whiteboardStatus === "全部" ? "selected" : ""}>全部</option><option ${state.whiteboardStatus === "未激活" ? "selected" : ""}>未激活</option><option ${state.whiteboardStatus === "已激活" ? "selected" : ""}>已激活</option></select><input placeholder="请选择客户状态"><button class="text-button" type="button" id="openWhiteboardTags">更多${state.whiteboardSelectedTags.length ? ` (${state.whiteboardSelectedTags.length})` : ""}</button></div>
        <div class="whiteboard-filter-actions"><button class="button primary" id="whiteboardApplyFilters" type="button">查询</button><button class="button secondary" id="whiteboardResetFilters" type="button">重置</button><button class="text-button" id="openWhiteboardAdvanced" type="button">${icon("sliders")}高级筛选</button></div>
      </section>
      <div class="whiteboard-actions"><button class="button primary" type="button" id="customerAllocate" ${canAllocate ? "" : "disabled"}>${icon("check")}分配给员工</button><button class="button secondary" type="button">${icon("user-transfer")}转为库存</button><span>共 ${rows.length} 条白板资源${state.selectedCustomerIds.length ? `，已选择 ${state.selectedCustomerIds.length} 条` : ""}</span></div>
      <section class="whiteboard-table-panel">
        <div class="table-wrap"><table class="data-table whiteboard-table"><thead><tr><th class="select-column"><input id="selectPageCustomers" type="checkbox" aria-label="全选" ${rows.length && rows.every(customer => state.selectedCustomerIds.includes(customer.id)) ? "checked" : ""}></th><th>标注</th><th>ID</th><th>称呼</th><th>性别</th><th>婚况</th><th>年龄</th><th>学历</th><th>收入</th><th>等级</th><th>城市</th><th>职业</th><th>客户状态</th><th>入库时间</th><th>标签</th><th>来源</th></tr></thead><tbody>${rows.map((customer, index) => `<tr data-customer-id="${escapeHtml(customer.id)}"><td class="select-column"><input type="checkbox" data-select-customer="${escapeHtml(customer.id)}" aria-label="选择${escapeHtml(customer.name)}" ${state.selectedCustomerIds.includes(customer.id) ? "checked" : ""} onclick="event.stopPropagation()"></td><td><span class="whiteboard-flag">⚑</span></td><td><button class="table-link" type="button" data-open-customer="${escapeHtml(customer.id)}">${escapeHtml(customer.id)}</button></td><td>${escapeHtml(customer.name || "—")}</td><td>${escapeHtml(customer.gender || "—")}</td><td>${escapeHtml(customer.maritalStatus || "—")}</td><td>${escapeHtml(customer.age || "—")}</td><td>${escapeHtml(customer.education || "—")}</td><td>${escapeHtml(customer.monthlyIncome || "—")}</td><td>${escapeHtml(customer.level || "普通客户")}</td><td>${escapeHtml(customer.city || "—")}</td><td>${escapeHtml(customer.occupation || "—")}</td><td><span class="pill gray">${escapeHtml(customer.stage || "未激活")}</span></td><td>${escapeHtml(String(customer.createdAt || customer.lastContactAt || "—").replace("T", " ").slice(0, 19))}</td><td><button class="whiteboard-add-tag" type="button">+ 增加标签</button></td><td>${escapeHtml(customer.source || "—")}</td></tr>`).join("")}</tbody></table></div>
        <footer class="pagination"><span>共 ${rows.length} 条</span><button class="page-button" type="button" disabled>‹</button><button class="page-button active" type="button">1</button><button class="page-button" type="button" disabled>›</button><span>20 条/页</span></footer>
      </section>
    </div>
    ${state.whiteboardTagModalOpen ? whiteboardTagModalView() : ""}
    ${state.whiteboardAdvancedOpen ? whiteboardAdvancedFilterView() : ""}
  </section>`;
}

function whiteboardTagModalView() {
  const groups = [
    ["无效组", ["不可推荐", "投诉风险", "没有离婚证", "不在本地", "内部员工", "重复资源", "兼职招聘", "不是本人", "非单身", "测试数据", "按揭法拍", "父母注册", "刷单", "网号ID", "空号", "隐身"]],
    ["精选用户组", ["才貌佳丽"]],
    ["其他", ["同行", "聋人", "残疾人", "50岁以上"]],
    ["销售", ["投放", "库存移出", "原有资源", "测试资源", "工厂", "APP预约咨询用户", "元宇时代", "爱聊汇资源", "空号补偿", "到店待跟进", "二邀到店", "一邀到店"]],
    ["管理组", []]
  ];
  const selected = new Set(state.whiteboardSelectedTags);
  return `<div class="modal-backdrop whiteboard-tag-backdrop" role="presentation"><section class="whiteboard-tag-modal" role="dialog" aria-modal="true" aria-label="筛选标签">
    <header><h2>筛选标签</h2><button type="button" id="closeWhiteboardTags" aria-label="关闭">×</button></header>
    <div class="whiteboard-tag-layout"><div class="whiteboard-tag-library"><h3>全部标签(<span id="whiteboardTagTotal">43</span>)</h3><div class="whiteboard-tag-search"><input id="whiteboardTagSearch" placeholder="请输入"><button type="button">${icon("search")}</button></div>
      <div class="whiteboard-tag-groups">${groups.map(([name, tags], groupIndex) => `<section class="whiteboard-tag-group"><div class="whiteboard-tag-group-title"><label><input type="checkbox" data-whiteboard-select-group="${groupIndex}"> 全选</label><i></i><strong>${escapeHtml(name)}</strong></div><div>${tags.map(tag => `<button class="${selected.has(tag) ? "selected" : ""}" type="button" data-whiteboard-tag="${escapeHtml(tag)}">${escapeHtml(tag)}</button>`).join("") || `<span class="whiteboard-tag-none">暂无标签</span>`}</div></section>`).join("")}</div>
    </div><aside class="whiteboard-tag-selected"><h3>已选(<span>${selected.size}</span>)</h3><button type="button" id="clearWhiteboardTags">清空</button><div>${[...selected].map(tag => `<button type="button" data-whiteboard-remove-tag="${escapeHtml(tag)}">${escapeHtml(tag)} ×</button>`).join("")}</div></aside></div>
  </section></div>`;
}

function whiteboardAdvancedFilterView() {
  const select = placeholder => `<select><option>${placeholder}</option></select>`;
  const range = (unit = "") => `<div class="advanced-range"><input placeholder="${unit}"><b>–</b><input placeholder="${unit}"></div>`;
  const dates = field => {
    const selectedRange = state.customerAdvancedDraftDateRanges[field] || { start: "", end: "" };
    const pickerOpen = state.customerAdvancedDatePickerOpen && state.customerAdvancedDatePickerField === field;
    return `<div class="advanced-date-range-control ${pickerOpen ? "open" : ""}"><button class="advanced-date-range-display" type="button" data-open-customer-advanced-date="${field}" aria-label="选择时间范围"><span>${selectedRange.start || "开始日期"}</span><b>→</b><span>${selectedRange.end || "结束日期"}</span><span class="advanced-date-range-icon">${icon("calendar")}</span></button>${pickerOpen ? customerAdvancedDateRangePickerView(field) : ""}</div>`;
  };
  const selectedGender = state.customerAdvancedDraftGender;
  const genderSelect = `<select id="customerGenderFilter" aria-label="性别">${customerGenderOptions.map(([value, label]) => `<option value="${value}" ${selectedGender === value ? "selected" : ""}>${label}</option>`).join("")}</select>`;
  const selectedMaritalStatus = state.customerAdvancedDraftMaritalStatus;
  const maritalStatusSelect = `<select id="customerMaritalStatusFilter" aria-label="婚况">${customerMaritalStatusOptions.map(([value, label]) => `<option value="${value}" ${selectedMaritalStatus === value ? "selected" : ""}>${label}</option>`).join("")}</select>`;
  const ageRange = `<div class="advanced-range age-range"><input id="customerAgeMin" type="number" min="0" max="150" step="1" inputmode="numeric" value="${escapeHtml(state.customerAdvancedDraftAgeMin)}" placeholder="岁" aria-label="最小年龄"><b>–</b><input id="customerAgeMax" type="number" min="0" max="150" step="1" inputmode="numeric" value="${escapeHtml(state.customerAdvancedDraftAgeMax)}" placeholder="岁" aria-label="最大年龄"></div>`;
  const heightRange = `<div class="advanced-range height-range"><input id="customerHeightMin" type="number" min="0" max="300" step="1" inputmode="numeric" value="${escapeHtml(state.customerAdvancedDraftHeightMin)}" placeholder="cm" aria-label="最小身高"><b>–</b><input id="customerHeightMax" type="number" min="0" max="300" step="1" inputmode="numeric" value="${escapeHtml(state.customerAdvancedDraftHeightMax)}" placeholder="cm" aria-label="最大身高"></div>`;
  const selectedEducations = selectedCustomerEducations(state.customerAdvancedDraftEducation);
  const selectedEducationLabels = customerEducationOptions
    .filter(([value]) => value !== "all" && selectedEducations.includes(value))
    .map(([, label]) => label);
  const educationChips = selectedEducationLabels.length
    ? selectedEducationLabels.map(label => `<span class="education-chip" data-remove-customer-education="${escapeHtml(label)}"><span>${escapeHtml(label)}</span><b aria-hidden="true">×</b></span>`).join("")
    : `<span class="education-multi-placeholder">全部</span>`;
  const educationMultiSelect = `<div class="education-multi-select ${state.customerEducationMenuOpen ? "open" : ""}" id="customerEducationFilter">
    <button class="education-multi-toggle" id="customerEducationToggle" type="button" aria-label="学历" aria-haspopup="listbox" aria-expanded="${state.customerEducationMenuOpen ? "true" : "false"}"><span class="education-multi-values">${educationChips}</span><span class="education-multi-arrow" aria-hidden="true"></span></button>
    ${state.customerEducationMenuOpen ? `<div class="education-multi-menu" role="listbox" aria-label="学历选项">${customerEducationOptions.map(([value, label]) => `<button class="education-multi-option ${value === "all" ? (!selectedEducations.length ? "selected" : "") : (selectedEducations.includes(value) ? "selected" : "")}" type="button" data-customer-education="${escapeHtml(value)}" aria-selected="${value === "all" ? !selectedEducations.length : selectedEducations.includes(value)}"><span>${escapeHtml(label)}</span><b aria-hidden="true">${value === "all" ? (!selectedEducations.length ? "✓" : "") : (selectedEducations.includes(value) ? "✓" : "")}</b></button>`).join("")}</div>` : ""}
  </div>`;
  const selectedUncontactedDays = state.customerAdvancedDraftUncontactedDays;
  const uncontactedDaysSelect = `<select id="customerUncontactedDaysFilter" aria-label="未联系天数">${customerUncontactedDaysOptions.map(([value, label]) => `<option value="${value}" ${selectedUncontactedDays === value ? "selected" : ""}>${label}</option>`).join("")}</select>`;
  const customUncontactedDays = selectedUncontactedDays === "custom"
    ? `<div class="advanced-custom-number"><input id="customerUncontactedDaysCustom" type="number" min="0" step="1" inputmode="numeric" value="${escapeHtml(state.customerAdvancedDraftUncontactedDaysCustom)}" placeholder="请输入天数" aria-label="自定义未联系天数"><span>天</span></div>`
    : "";
  const dialStatusSelect = `<select id="customerDialStatusFilter" aria-label="拨打状态">${customerDialStatusOptions.map(([value, label]) => `<option value="${value}" ${state.customerAdvancedDraftDialStatus === value ? "selected" : ""}>${label}</option>`).join("")}</select>`;
  const avatarSelect = `<select id="customerAvatarFilter" aria-label="有无头像">${customerAvatarOptions.map(([value, label]) => `<option value="${value}" ${state.customerAdvancedDraftAvatar === value ? "selected" : ""}>${label}</option>`).join("")}</select>`;
  const ownerCascade = customerOwnerCascadeControl("advanced", state.customerAdvancedDraftOwnerSelection, state.customerAdvancedOwnerCascadeOpen);
  const collaboratorCascade = customerOwnerCascadeControl("advanced-collaborator", state.customerAdvancedDraftCollaboratorSelection, state.customerAdvancedCollaboratorCascadeOpen);
  return `<div class="modal-backdrop whiteboard-advanced-backdrop" role="presentation"><section class="whiteboard-advanced-modal" role="dialog" aria-modal="true" aria-label="高级筛选">
    <header><h2>高级筛选</h2><button type="button" id="closeWhiteboardAdvanced" aria-label="关闭">×</button></header>
    <div class="whiteboard-advanced-body">
      <div class="advanced-wide-row"><strong>客户等级：</strong><div class="advanced-checks">${["5心", "4心", "3心", "2心"].map(item => `<label><input type="checkbox">${item}</label>`).join("")}</div></div>
      <label class="advanced-full"><strong>客户类型：</strong><select><option>请选择</option><option>非会员·未注册</option><option>非会员·已注册</option><option>会员</option></select></label>
      <label class="advanced-full"><strong>客户状态：</strong><select><option>全部</option>${customerStatusOptions.map(option => `<option>${option}</option>`).join("")}</select></label>
      <div class="advanced-wide-row"><strong>下次跟进时间：</strong><div class="advanced-checks">${["今天", "明天", "本周", "下周", "本月", "下月"].map(item => `<label><input type="checkbox">${item}</label>`).join("")}</div><div class="advanced-custom-date"><span>自定义开始时间</span><b>→</b><span>自定义结束时间</span>${icon("calendar")}</div></div>
      <div class="advanced-two-column"><label><strong>未联系天数：</strong><div class="uncontacted-days-control">${uncontactedDaysSelect}${customUncontactedDays}</div></label><label><strong>会员来源：</strong><input placeholder="请选择会员来源"></label></div>
      <section class="advanced-profile"><h3>客户资料详情：</h3><div class="advanced-profile-grid">
        <label><span>性别：</span>${genderSelect}</label><label><span>年龄：</span>${ageRange}</label><label><span>身高：</span>${heightRange}</label><label><span>学历：</span>${educationMultiSelect}</label>
        <label><span>收入：</span>${range("元")}</label><label><span>婚况：</span>${maritalStatusSelect}</label><label><span>籍贯：</span>${select("请选择")}</label><label><span>工作地：</span>${select("请选择")}</label>
        <label><span>职业：</span><input placeholder="请选择"></label><label><span>购车：</span>${select("请选择")}</label><label><span>购房：</span>${select("请选择")}</label><label><span>性格：</span><input placeholder="请选择"></label>
        <label><span>兴趣爱好：</span><input placeholder="请选择"></label>
      </div></section>
      <div class="advanced-time-grid"><label><strong>客户注册时间：</strong>${dates("registration")}</label><label><strong>最近登录时间：</strong>${dates("lastLogin")}</label><label><strong>首次分配时间：</strong>${dates("firstAllocation")}</label><label><strong>所属人：</strong>${ownerCascade}</label><label><strong>协作人：</strong>${collaboratorCascade}</label><label><strong>最后跟进时间：</strong>${dates("lastFollowUp")}</label><label class="advanced-note"><strong>备注信息：</strong><input placeholder="请输入"></label><label><strong>拨打状态：</strong>${dialStatusSelect}</label><label><strong>有无头像：</strong>${avatarSelect}</label></div>
    </div>
    <footer><button class="button secondary" id="cancelWhiteboardAdvanced" type="button">取消</button><button class="button primary" id="queryWhiteboardAdvanced" type="button">查询</button></footer>
  </section></div>`;
}

function inventoryResourceView() {
  const query = state.customerSearch.trim().toLowerCase();
  const rows = customers.filter(customer => !query || [customer.id, customer.phone, customer.name, customer.company].join(" ").toLowerCase().includes(query));
  return `<section class="page inventory-resource-page">
    ${subnav(["首页", "库存资源"], "库存资源", ["首页", "库存资源"])}
    <div class="page-content">
      <section class="inventory-filter-panel">
        <div class="inventory-filter-row"><span>筛选条件：</span><input id="customerSearch" value="${escapeHtml(state.customerSearch)}" placeholder="ID/手机号"><input placeholder="姓名/昵称/备注"><select><option>全部</option><option>未激活</option><option>已激活</option></select><input placeholder="请选择客户状态"><div class="inventory-date-range"><span>移入开始时间</span><b>→</b><span>移入结束时间</span>${icon("calendar")}</div><button class="text-button" id="openInventoryTags" type="button">更多</button></div>
        <div class="inventory-filter-actions"><button class="button primary" id="applyCustomerFilters" type="button">查询</button><button class="button secondary" id="resetCustomerFilters" type="button">重置</button><button class="text-button" id="openInventoryAdvanced" type="button">${icon("sliders")}高级筛选</button></div>
      </section>
      <label class="inventory-wechat"><input type="checkbox"> 添加微信客户</label>
      <div class="inventory-actions"><button class="button primary" type="button">${icon("sliders")}资源调配</button><button class="button secondary" type="button">${icon("users")}领取客户</button><button class="button secondary" type="button">${icon("user-transfer")}移入公海</button></div>
      <section class="inventory-table-panel"><div class="table-wrap"><table class="data-table inventory-table"><thead><tr><th class="select-column"><input type="checkbox" aria-label="全选"></th><th>标注</th><th>ID</th><th>客户姓名/昵称</th><th>性别</th><th>婚否</th><th>年龄</th><th>学历</th><th>收入</th><th>等级</th><th>城市</th><th>未联系天数</th><th>归属人</th><th>邀约人</th><th>协作人</th><th>服务人</th><th>客户状态</th><th>库存天数</th><th class="operation-column">操作</th></tr></thead><tbody>${rows.map((customer, index) => `<tr data-customer-id="${escapeHtml(customer.id)}"><td class="select-column"><input type="checkbox" aria-label="选择${escapeHtml(customer.name)}" onclick="event.stopPropagation()"></td><td><span class="inventory-flag">⚑</span></td><td><button class="inventory-id" type="button" data-open-customer="${escapeHtml(customer.id)}">${escapeHtml(customer.id)}</button></td><td><div class="inventory-customer"><span class="person-avatar">${escapeHtml(customer.name[0])}</span><span>昵称：${escapeHtml(customer.name || customer.phone || "—")}</span></div></td><td>${escapeHtml(customer.gender || "—")}</td><td>${escapeHtml(customer.maritalStatus || "—")}</td><td>${escapeHtml(customer.age || "—")}</td><td>${escapeHtml(customer.education || "—")}</td><td>${escapeHtml(customer.monthlyIncome || "—")}</td><td>${escapeHtml(customer.level || "—")}</td><td>${escapeHtml(customer.city || "—")}</td><td>${escapeHtml(String(customer.uncontactedDays || 0))}</td><td>${escapeHtml(customer.owner || "管理员")}</td><td>—</td><td>—</td><td>—</td><td>${escapeHtml(customer.stage || "未激活")}</td><td>${escapeHtml(String(customer.inventoryDays || "—"))}</td><td class="operation-column"><div class="table-actions"><button class="inventory-claim" type="button" data-inventory-claim="${escapeHtml(customer.id)}">领取</button></div></td></tr>`).join("")}</tbody></table></div><footer class="pagination"><span>共 ${rows.length} 条</span><button class="page-button" disabled>‹</button><button class="page-button active">1</button><button class="page-button" disabled>›</button><span>20 条/页</span></footer></section>
    </div>
    ${state.whiteboardTagModalOpen ? whiteboardTagModalView() : ""}
    ${state.whiteboardAdvancedOpen ? whiteboardAdvancedFilterView() : ""}
  </section>`;
}

function serviceLibraryView() {
  const query = state.customerSearch.trim().toLowerCase();
  const sourceRows = customers.filter(customer => {
    const matchesQuery = !query || [customer.id, customer.phone, customer.name, customer.company, customer.note].join(" ").toLowerCase().includes(query);
    const matchesScope = state.serviceScope === "all"
      || (state.serviceScope === "mine" && customer.owner === currentOwner())
      || (state.serviceScope === "subordinates" && customer.owner !== currentOwner());
    return matchesQuery && matchesScope;
  });
  const scenarios = ["全部", "今日关怀会员", "逾期会员", "待开启会员", "今日断分", "即将到期", "过期未关单", "服务期内", "已过期"];
  const rows = state.serviceScenario === "全部" ? sourceRows : sourceRows.filter(customer => {
    if (state.serviceScenario === "服务期内") return customer.stage === "已成交";
    if (state.serviceScenario === "已过期" || state.serviceScenario === "逾期会员") return customer.stage === "已流失";
    if (state.serviceScenario === "待开启会员") return customer.stage === "需求确认";
    return true;
  });
  return `<section class="page service-library-page">
    ${subnav(["首页", "服务库"], "服务库", ["首页", "服务库"])}
    <div class="page-content">
      <div class="service-scope-tabs">${[["all", "全部会员"], ["subordinates", "下属会员"], ["mine", "我的会员"]].map(([scope, label]) => `<button class="${state.serviceScope === scope ? "active" : ""}" type="button" data-service-scope="${scope}">${label}</button>`).join("")}</div>
      <section class="service-filter-panel">
        <div class="service-scenarios"><strong>场景：</strong>${scenarios.map(label => `<button class="${state.serviceScenario === label ? "active" : ""}" type="button" data-service-scenario="${label}">${label}</button>`).join("")}<button class="text-button" type="button">更多</button></div>
        <div class="service-filter-row"><strong>筛选条件：</strong><input id="customerSearch" value="${escapeHtml(state.customerSearch)}" placeholder="ID/手机号"><input placeholder="姓名/昵称/备注"><input placeholder="所属人"><div class="service-number-range"><input type="number" placeholder="最小跟进"><b>–</b><input type="number" placeholder="最大跟进"></div><div class="service-date-range"><input type="date" aria-label="分配开始时间"><b>–</b><input type="date" aria-label="分配结束时间"></div></div>
        <div class="service-filter-actions"><button class="button primary" id="applyCustomerFilters" type="button">查询</button><button class="button secondary" id="resetCustomerFilters" type="button">重置</button><button class="text-button" id="openServiceAdvanced" type="button">${icon("sliders")}高级筛选</button></div>
      </section>
      <div class="service-batch-area"><label><input type="checkbox"> 添加微信客户</label><div><button class="button primary" type="button">${icon("check")}资源调配</button><button class="button secondary" type="button">${icon("message")}发送消息</button><button class="button secondary" type="button">${icon("table-grid")}自定义表头</button></div><p>*红色ID为待开启会员</p></div>
      <section class="service-table-panel"><div class="table-wrap">${rows.length ? `<table class="data-table service-library-table"><thead><tr><th class="select-column"><input type="checkbox" aria-label="全选"></th><th>标注</th><th>ID</th><th>客户姓名/昵称</th><th>性别</th><th>婚况</th><th>年龄</th><th>学历</th><th>收入</th><th>等级</th><th>城市</th><th>跟进次数</th><th>未联系天数</th><th>归属人</th><th>服务人</th><th>客户状态</th><th>分配时间</th><th>到期时间</th><th>标签</th><th>来源</th><th class="operation-column">操作</th></tr></thead><tbody>${rows.map(customer => `<tr data-customer-id="${escapeHtml(customer.id)}"><td><input type="checkbox" aria-label="选择${escapeHtml(customer.name)}" onclick="event.stopPropagation()"></td><td>⚑</td><td><button class="table-link" type="button" data-open-customer="${escapeHtml(customer.id)}">${escapeHtml(customer.id)}</button></td><td>${escapeHtml(customer.name || "—")}</td><td>${escapeHtml(customer.gender || "—")}</td><td>${escapeHtml(customer.maritalStatus || "—")}</td><td>${escapeHtml(customer.age || "—")}</td><td>${escapeHtml(customer.education || "—")}</td><td>${escapeHtml(customer.monthlyIncome || "—")}</td><td>${escapeHtml(customer.level || "—")}</td><td>${escapeHtml(customer.city || "—")}</td><td>${escapeHtml(String(customer.followUpCount || 0))}</td><td>${escapeHtml(String(customer.uncontactedDays || "—"))}</td><td>${escapeHtml(customer.owner || "—")}</td><td>${escapeHtml(customer.servicePerson || "—")}</td><td>${escapeHtml(customer.stage || "—")}</td><td>${escapeHtml(String(customer.createdAt || "—").replace("T", " ").slice(0, 19))}</td><td>—</td><td>${(customer.tags || []).map(tag => `<span class="tag">${escapeHtml(tag)}</span>`).join("") || "—"}</td><td>${escapeHtml(customer.source || "—")}</td><td class="operation-column"><button class="button ghost" type="button" data-open-customer="${escapeHtml(customer.id)}">跟进</button></td></tr>`).join("")}</tbody></table>` : `<div class="service-empty"><span class="empty-icon">${icon("users")}</span><strong>暂无数据</strong></div>`}</div><footer class="pagination"><span>共 ${rows.length} 条</span><button class="page-button" disabled>‹</button><button class="page-button active">1</button><button class="page-button" disabled>›</button><span>20 条/页</span></footer></section>
    </div>${state.serviceAdvancedOpen ? serviceAdvancedFilterView() : ""}
  </section>`;
}

function serviceAdvancedFilterView() {
  const select = placeholder => `<select><option>${placeholder}</option></select>`;
  const range = unit => `<div class="advanced-range"><input placeholder="${unit}"><b>–</b><input placeholder="${unit}"></div>`;
  const dates = () => `<div class="advanced-date-range"><input type="date"><b>→</b><input type="date"></div>`;
  return `<div class="modal-backdrop whiteboard-advanced-backdrop service-advanced-backdrop" role="presentation"><section class="whiteboard-advanced-modal service-advanced-modal" role="dialog" aria-modal="true" aria-label="服务库高级筛选">
    <header><h2>高级筛选</h2><button type="button" id="closeServiceAdvanced" aria-label="关闭">×</button></header>
    <div class="whiteboard-advanced-body">
      <div class="advanced-wide-row"><strong>客户等级：</strong><div class="advanced-checks">${["5心", "4心", "3心", "2心"].map(item => `<label><input type="checkbox">${item}</label>`).join("")}</div></div>
      <label class="advanced-full"><strong>客户类型：</strong>${select("请选择")}</label>
      <label class="advanced-full"><strong>客户状态：</strong>${select("全部")}</label>
      <div class="advanced-wide-row"><strong>下次跟进时间：</strong><div class="advanced-checks">${["今天", "明天", "本周", "下周", "本月", "下月"].map(item => `<label><input type="checkbox">${item}</label>`).join("")}</div><div class="advanced-custom-date"><span>自定义开始时间</span><b>→</b><span>自定义结束时间</span>${icon("calendar")}</div></div>
      <div class="advanced-two-column"><label><strong>未联系天数：</strong>${select("请选择")}</label><label><strong>会员来源：</strong><input placeholder="请选择会员来源"></label></div>
      <section class="advanced-profile"><h3>客户资料详情：</h3><div class="advanced-profile-grid">
        <label><span>性别：</span>${select("请选择")}</label><label><span>年龄：</span>${range("岁")}</label><label><span>身高：</span>${range("cm")}</label><label><span>学历：</span>${select("请选择")}</label>
        <label><span>收入：</span>${range("元")}</label><label><span>婚况：</span>${select("请选择")}</label><label><span>籍贯：</span>${select("请选择")}</label><label><span>工作地：</span>${select("请选择")}</label>
        <label><span>职业：</span><input placeholder="请选择"></label><label><span>购车：</span>${select("请选择")}</label><label><span>购房：</span>${select("请选择")}</label><label><span>性格：</span><input placeholder="请选择"></label><label><span>兴趣爱好：</span><input placeholder="请选择"></label>
      </div></section>
      <div class="advanced-time-grid"><label><strong>客户注册时间：</strong>${dates()}</label><label><strong>最近登录时间：</strong>${dates()}</label><label><strong>首次分配时间：</strong>${dates()}</label><label><strong>归属人：</strong><input placeholder="请选择"></label><label><strong>协作人：</strong><input placeholder="请选择"></label><label><strong>最后跟进时间：</strong>${dates()}</label><label class="advanced-note"><strong>备注信息：</strong><input placeholder="请输入"></label></div>
    </div>
    <footer><button class="button secondary" id="cancelServiceAdvanced" type="button">取消</button><button class="button primary" id="queryServiceAdvanced" type="button">查询</button></footer>
  </section></div>`;
}

function expiredVipLibraryView() {
  const query = state.customerSearch.trim().toLowerCase();
  const rows = customers.filter(customer => customer.stage === "已流失" && (!query || [customer.id, customer.phone, customer.name, customer.note, customer.owner].join(" ").toLowerCase().includes(query)));
  return `<section class="page expired-vip-page">
    ${subnav(["首页", "过期VIP库"], "过期VIP库", ["首页", "过期VIP库"])}
    <div class="page-content">
      <section class="expired-vip-filter">
        <div class="expired-vip-filter-row"><strong>筛选条件：</strong><input id="customerSearch" value="${escapeHtml(state.customerSearch)}" placeholder="ID/手机号"><input placeholder="姓名/昵称/备注"><input placeholder="前归属人"><input placeholder="前服务人"><div class="expired-vip-date"><input type="date" aria-label="关单开始时间"><b>→</b><input type="date" aria-label="关单结束时间"></div></div>
        <div class="expired-vip-filter-actions"><button class="button primary" id="applyCustomerFilters" type="button">查询</button><button class="button secondary" id="resetCustomerFilters" type="button">重置</button><button class="text-button" id="openExpiredVipAdvanced" type="button">${icon("sliders")}高级筛选</button></div>
      </section>
      <div class="expired-vip-actions"><div class="expired-consent"><label><input type="radio" name="expiredConsent" checked> 未取得用户同意</label><label><input type="radio" name="expiredConsent"> 取得用户同意</label></div><div><button class="button primary" type="button">${icon("check")}资源调配</button><button class="button secondary" type="button">${icon("users")}领取客户</button></div></div>
      <section class="expired-vip-table-panel"><div class="table-wrap">${rows.length ? `<table class="data-table expired-vip-table"><thead><tr><th class="select-column"><input type="checkbox" aria-label="全选"></th><th>标注</th><th>ID</th><th>客户姓名/昵称</th><th>性别</th><th>婚况</th><th>年龄</th><th>学历</th><th>收入</th><th>等级</th><th>城市</th><th>未联系天数</th><th>前归属人</th><th>前服务人</th><th>客户状态</th><th>用户是否同意</th><th>关单时间</th><th class="operation-column">操作</th></tr></thead><tbody>${rows.map(customer => `<tr data-customer-id="${escapeHtml(customer.id)}"><td><input type="checkbox" aria-label="选择${escapeHtml(customer.name)}"></td><td>⚑</td><td><button class="table-link expired-vip-id" type="button" data-open-customer="${escapeHtml(customer.id)}">${escapeHtml(customer.id)}</button></td><td>${escapeHtml(customer.name || "—")}</td><td>${escapeHtml(customer.gender || "—")}</td><td>${escapeHtml(customer.maritalStatus || "—")}</td><td>${escapeHtml(customer.age || "—")}</td><td>${escapeHtml(customer.education || "—")}</td><td>${escapeHtml(customer.monthlyIncome || "—")}</td><td>${escapeHtml(customer.level || "—")}</td><td>${escapeHtml(customer.city || "—")}</td><td>${escapeHtml(String(customer.uncontactedDays || "—"))}</td><td>${escapeHtml(customer.previousOwner || customer.owner || "—")}</td><td>${escapeHtml(customer.servicePerson || "—")}</td><td>${escapeHtml(customer.stage || "—")}</td><td>未取得同意</td><td>${escapeHtml(String(customer.updatedAt || customer.lastContactAt || "—").replace("T", " ").slice(0, 19))}</td><td class="operation-column"><button class="button ghost" type="button" data-open-customer="${escapeHtml(customer.id)}">查看</button></td></tr>`).join("")}</tbody></table>` : `<div class="expired-vip-empty"><span class="empty-icon">${icon("users")}</span><strong>暂无数据</strong></div>`}</div><footer class="pagination"><span>共 ${rows.length} 条</span><button class="page-button" disabled>‹</button><button class="page-button active">1</button><button class="page-button" disabled>›</button><span>20 条/页</span></footer></section>
    </div>${state.serviceAdvancedOpen ? serviceAdvancedFilterView() : ""}
  </section>`;
}

function customerFeatureView(section) {
  return `<section class="page"><div class="page-content"><section class="data-panel"><div class="data-toolbar"><div class="panel-title"><h2>${escapeHtml(section)}</h2><span>客户管理功能</span></div></div><div class="empty-state"><span class="empty-icon">${icon("users")}</span><strong>${escapeHtml(section)}</strong><p>该功能入口已开放，可在此继续管理相关客户数据。</p></div></section></div></section>`;
}

function customersView() {
  if (state.customerDetailId) return customerDetailView(state.customerDetailId);
  if (state.customerSection === "公海列表") return customerPoolView();
  if (state.customerSection === "诚意资源") return sincereResourceView();
  if (state.customerSection === "白板列表") return whiteboardListView();
  if (state.customerSection === "库存资源") return inventoryResourceView();
  if (state.customerSection === "服务库") return serviceLibraryView();
  if (state.customerSection === "过期VIP库") return expiredVipLibraryView();
  if (state.customerSection === "客户导入") return customerImportView();
  if (state.customerSection === "标签管理") return customerTagView();
  if (state.customerSection !== "客户列表") return customerFeatureView(state.customerSection);
  return customerListView();
}

function customerDetailView(id) {
  const customer = [...customers, ...state.poolCustomers].find(item => String(item.id) === String(id));
  if (!customer) {
    state.customerDetailId = null;
    return customerListView();
  }
  const value = item => escapeHtml(String(item ?? ""));
  const contactVisible = customer.contactVisible ?? (isAdmin() || customer.owner === currentOwner());
  const headerPhone = contactVisible ? (customer.phone || "") : maskPhoneDisplay(customer.phone);
  const cell = (label, item, required = false) => label
    ? `<div class="profile-reference-field"><dt>${required ? "*" : ""}${label}</dt><dd>${value(item)}</dd></div>`
    : '<div class="profile-reference-gap" aria-hidden="true"></div>';
  const privateCell = (label, item) => {
    const displayValue = contactVisible
      ? item
      : (label === "电话号码" ? maskPhoneDisplay(item) : label === "微信号" ? maskWechatDisplay(item) : item);
    return `<div class="profile-reference-field"><dt>${label}</dt><dd>${item ? (contactVisible ? `<span class="profile-private-value">${value(displayValue)}</span>` : `<span class="profile-private-value profile-private-mask">${value(displayValue)}<span class="profile-private-action">无权限查看</span></span>`) : ""}</dd></div>`;
  };
  const birthday = customer.birthday ? `${customer.birthday}${customer.age != null && customer.age !== "" ? `【${customer.age}岁】` : ""}` : "";
  // Each group is one complete visual row in the reference, including empty cells.
  const basicRows = [
    [cell("姓名", customer.name, true), cell("性别", customer.gender, true), cell("年龄", customer.age, true)],
    [privateCell("电话号码", customer.phone), cell("生日", birthday, true), cell("身高", customer.height, true)],
    [cell("职业", customer.occupation), cell("婚况", customer.maritalStatus, true), cell("学历", customer.education, true)],
    [cell("工作地", customer.workLocation, true), cell("月收入", customer.monthlyIncome, true), cell("籍贯", customer.nativePlace)],
    [privateCell("微信号", customer.wechat), cell("购房情况", customer.housing), cell("购车情况", customer.car)],
    [cell("认证情况", customer.certificationStatus), cell("房车情况", customer.vehicleHousing), cell("")],
    [cell("家庭情况", customer.familyStatus || customer.family), cell(""), cell("")],
    [cell("子女情况", customer.childrenStatus || customer.children), cell(""), cell("")],
    [cell("客户来源", customer.source), cell("注册时间", customer.createdAt ? formatDateTime(customer.createdAt) : ""), cell("注册次数", customer.registrationCount)]
  ];
  const matingInfo = [
    ["年龄范围", customer.matchAgeRange ?? "不限-不限"],
    ["婚况范围", customer.matchMaritalStatus ?? "不限"],
    ["身高范围", customer.matchHeightRange ?? "不限-不限"],
    ["学历范围", customer.matchEducation],
    ["月收入", customer.matchMonthlyIncome ?? "不限-不限"],
    ["最在意对方", customer.matchMostImportant],
    ["性格", customer.matchPersonality],
    ["下一代需求", customer.matchChildren],
    ["不希望TA", customer.matchDealbreakers]
  ];
  const registrationRecords = (state.customerRegistrationEvents[customer.id] || []).map(event => ({
    customerId: customer.id,
    customer: customer.name,
    type: "系统提示",
    detail: `客户第${event.registrationNumber}次注册`,
    owner: event.operator || "系统",
    at: event.createdAt
  }));
  const assignmentRecords = (state.customerAssignmentEvents[customer.id] || []).map(event => ({
    customerId: customer.id,
    customer: customer.name,
    type: "客户分配",
    detail: `${customerOwnerDisplay(event.previousOwner)} → ${customerOwnerDisplay(event.owner)}`,
    owner: event.operator || "系统",
    at: event.assignedAt
  }));
  const records = [...customerActivityRows().filter(row => String(row.customerId) === String(customer.id) || (!row.customerId && row.type === "跟进任务" && row.customer === customer.name)), ...registrationRecords, ...assignmentRecords]
    .sort((a, b) => new Date(b.at || 0) - new Date(a.at || 0));
  const portrait = `<div class="profile-reference-portrait"><div class="profile-reference-placeholder" role="img" aria-label="客户默认头像">${icon("users")}</div><span class="profile-reference-caption">${value(customer.name)}</span></div>`;
  const noteText = [customer.note, customer.remark].filter(Boolean).join("\n");
  const noteTime = customer.updatedAt || customer.createdAt;
  const profile = `<div class="profile-reference-overview">${portrait}<section class="profile-reference-basic"><h3>基本信息</h3><dl class="profile-reference-basic-grid">${basicRows.flat().join("")}</dl></section><section class="profile-reference-mating"><h3>择偶信息</h3><dl>${matingInfo.map(([label, item]) => cell(label, item)).join("")}</dl></section></div><section class="profile-reference-notes"><h3>备注</h3><dl><div><dt>备注信息</dt><dd><span>${value(noteText)}</span>${noteTime ? `<time>${value(formatDateTime(noteTime))}</time>` : ""}</dd></div></dl></section><section class="profile-reference-photos"><h3>图片</h3><p>暂无照片</p></section><div class="customer-detail-tools"><button type="button" data-customer-opening>开场白</button><button type="button" data-customer-followup>写跟进</button></div>`;
  const moreActions = ["增加协作", "转为库存", "移入公海", "发邀请券", "添加至重点客户", "注销用户"];
  const moreMenu = `<div class="customer-more-actions"><button class="button secondary customer-more-trigger" type="button" data-customer-more-toggle="${value(customer.id)}" aria-label="更多操作" aria-haspopup="menu" aria-expanded="false">${icon("more")}</button><div class="customer-more-menu" data-customer-more-menu="${value(customer.id)}" role="menu">${moreActions.map((label, index) => `<button type="button" role="menuitem" data-customer-more-action="${value(index)}" data-customer-id="${value(customer.id)}">${value(label)}</button>`).join("")}</div></div>`;
  const follow = `<section class="customer-profile-section"><h3>跟进记录</h3><div class="customer-profile-timeline">${records.map(row => `<div><time>${value(formatDateTime(row.at))}</time><p><strong>${value(row.type)}</strong>　${value(row.detail)}<br><small>操作人：${value(row.owner)}</small></p></div>`).join("") || '<p class="customer-profile-empty">暂无客户操作记录</p>'}</div></section>`;
  const profileGender = customer.gender || "—";
  const profileAge = customer.age ? `${customer.age}岁` : "—岁";
  const profileEducation = customer.education || "—";
  const isWhiteboardCustomer = state.customerSection === "白板列表";
  const isAssignedToEmployee = !["", "白板", "公海"].includes(String(customer.owner || "").trim());
  const profileOwner = isWhiteboardCustomer ? "" : customerOwnerDisplay(customer.owner);
  const profileCollaborator = isAssignedToEmployee ? customer.collaborator || "" : "";
  const profileAllocationTime = isAssignedToEmployee && customer.lastAllocationAt ? formatDateTime(customer.lastAllocationAt) : "";
  const profileActions = isWhiteboardCustomer
    ? `<button class="button secondary" type="button" data-customer-more-action="1" data-customer-more-label="库存" data-customer-id="${value(customer.id)}">转为库存</button>${isAdmin() ? `<button class="button secondary" type="button" data-allocate-profile-customer="${value(customer.id)}">资源调配</button>` : ""}<button class="button secondary" type="button" data-customer-more-action="4" data-customer-more-label="诚意库" data-customer-id="${value(customer.id)}">＋ 添加至诚意库</button><button class="button secondary" type="button" data-next-customer="${value(customer.id)}" data-next-customer-label="下一个客户">下一个客户 ›</button>`
    : `<button class="button secondary" type="button" data-new-order-customer="${value(customer.id)}">＋ 新订单</button>${contactVisible ? `<button class="button secondary" type="button" data-call-name="${value(customer.name)}">☎ 拨打</button><button class="button secondary" type="button" data-message-name="${value(customer.name)}">▣ 消息</button>` : ""}${isAdmin() ? `<button class="button secondary" type="button" data-allocate-profile-customer="${value(customer.id)}">资源调配</button>` : ""}<button class="button secondary" type="button" data-next-customer="${value(customer.id)}">下个客户 ›</button>${moreMenu}`;
  const profileHeader = `<div class="profile-reference-header"><div class="profile-reference-title-row"><h1>${value(headerPhone)} <span>[${value(profileGender)} ${value(profileAge)} ${value(profileEducation)}]</span>　【${value(customer.id)}】 <span class="profile-header-icons">${icon("star")} ${icon(contactVisible ? "lock-open" : "lock")} ${icon("flag")}</span></h1><span class="profile-reference-status">● 未激活</span></div><div class="profile-reference-second-row"><div class="profile-reference-header-meta"><div class="profile-reference-meta-row profile-reference-tags-row"><span>客户标签：</span><button class="profile-add-tag" type="button" data-add-profile-tag="${value(customer.id)}">+ 增加标签</button></div><div class="profile-reference-meta-row"><span>归属人：<b>${value(profileOwner)}</b></span><span>协作人：<b>${value(profileCollaborator)}</b></span><span>分配时间：<b>${value(profileAllocationTime)}</b></span><span>下次跟进时间：<b>${value(customer.nextFollowAt ? formatDateTime(customer.nextFollowAt) : "")}</b></span></div></div><div class="profile-reference-actions">${profileActions}</div></div></div>`;
  return `<section class="page customer-profile-page profile-reference-page">${profileHeader}<div class="customer-profile-tabs"><button class="${state.customerDetailTab === "profile" ? "active" : ""}" type="button" data-customer-detail-tab="profile">资料详情</button><button class="${state.customerDetailTab === "follow" ? "active" : ""}" type="button" data-customer-detail-tab="follow">跟进记录 (${records.length})</button><button type="button" disabled>约会安排</button><button type="button" disabled>推荐记录</button>${contactVisible ? `<button class="customer-profile-tab-edit" type="button" data-edit-profile-customer="${value(customer.id)}">编辑</button>` : ""}</div><div class="profile-reference-content">${state.customerDetailTab === "follow" ? follow : profile}</div></section>`;
}

function tasksView() {
  const columns = [
    { key: "overdue", title: "已逾期" }, { key: "today", title: "今天" }, { key: "upcoming", title: "即将开始" }, { key: "done", title: "已完成" }
  ];
  const content = state.taskMode === "calendar"
    ? calendarView()
    : state.taskMode === "activity"
      ? followUpRecordsView()
    : `<div class="kanban">${columns.map(column => { const items = tasks.filter(task => task.status === column.key); return `<section class="kanban-column"><header class="kanban-header"><span class="kanban-title"><i class="kanban-dot"></i>${column.title}</span><span class="kanban-count">${items.length}</span></header>${items.map(task => `<article class="kanban-card" data-task-card="${task.id}"><span class="pill ${task.priority === "逾期" ? "red" : task.priority === "高" || task.priority === "紧急" ? "amber" : task.done ? "green" : "gray"}">${task.priority}</span><h3>${task.title}</h3><p>${task.customer} · ${task.type}</p><footer class="kanban-card-footer"><span>${icon("clock")}${task.due}</span><span class="person-avatar">${task.owner[0]}</span></footer></article>`).join("") || `<div class="empty-state" style="padding:35px 8px">暂无任务</div>`}</section>`; }).join("")}</div>`;
  const active = state.taskMode === "calendar" ? "日历视图" : state.taskMode === "activity" ? "跟进记录" : "任务看板";
  return `<section class="page">
    ${subnav(["任务看板", "日历视图", "跟进记录"], active, ["任务看板", "日历视图", "跟进记录"])}
    <div class="page-content">
      ${pageHeading(`<button class="button secondary" id="toggleTaskView" type="button">${icon("calendar")}${state.taskMode === "calendar" ? "任务看板" : "日历视图"}</button><button class="button primary" id="newTask" type="button">${icon("plus")}新建任务</button>`)}
      ${content}
    </div>
  </section>`;
}

function callRecordsView() {
  const keyword = state.callKeyword.trim().toLowerCase();
  const nameKeyword = state.callNameKeyword.trim().toLowerCase();
  const records = calls.filter(call => (!keyword || `${call.customerId || ""} ${call.phone || ""}`.toLowerCase().includes(keyword)) && (!nameKeyword || (call.customer || "").toLowerCase().includes(nameKeyword)) && (state.callAgentFilter === "全部坐席" || call.agent === state.callAgentFilter || call.owner === state.callAgentFilter) && (state.callDirectionFilter === "全部" || call.direction === state.callDirectionFilter) && (state.callStatusFilter === "全部" || call.status === state.callStatusFilter));
  const agents = [...new Set(calls.map(call => call.agent || call.owner).filter(Boolean))].sort((a, b) => a.localeCompare(b, "zh-CN"));
  return `<section class="page call-record-page"><div class="page-content"><section class="call-record-filter">
    <label><b>ID/手机号：</b><input id="callKeyword" value="${escapeHtml(state.callKeyword)}" placeholder="请输入用户Id或者手机号"></label><label><b>昵称/姓名：</b><input id="callNameKeyword" value="${escapeHtml(state.callNameKeyword)}" placeholder="请输入用户昵称或姓名"></label><label><b>呼出时间：</b><span class="call-date-range"><input type="date" aria-label="开始日期"><i>→</i><input type="date" aria-label="结束日期"></span></label>
    <label><b>处理人：</b><select id="callAgentFilter"><option value="全部坐席">请选择</option>${agents.map(agent => `<option ${agent === state.callAgentFilter ? "selected" : ""}>${escapeHtml(agent)}</option>`).join("")}</select></label><label><b>呼叫类型：</b><select id="callDirectionFilter"><option>全部</option><option ${state.callDirectionFilter === "呼出" ? "selected" : ""}>呼出</option><option ${state.callDirectionFilter === "呼入" ? "selected" : ""}>呼入</option></select></label><label><b>通话状态：</b><select id="callStatusFilter"><option>全部</option><option ${state.callStatusFilter === "已接通" ? "selected" : ""}>已接通</option><option ${state.callStatusFilter === "未接通" ? "selected" : ""}>未接通</option><option ${state.callStatusFilter === "待回拨" ? "selected" : ""}>待回拨</option></select></label>
    <label><b>呼叫时长：</b><select><option>请选择</option></select></label><label><b>通话时长：</b><select><option>请选择</option></select></label><div class="call-filter-actions"><button class="button primary" id="applyCallFilters" type="button">${icon("search")}查询</button><button class="button secondary" id="resetCallFilters" type="button">${icon("repeat")}重置</button></div>
  </section><section class="call-record-panel"><div class="call-record-tools"><label>AI分析展开预览 <input id="callAiPreview" type="checkbox" ${state.callAiPreview ? "checked" : ""}><span></span></label><button class="button primary" type="button">通话相关数据</button></div><div class="table-wrap"><table class="call-record-table"><thead><tr><th>用户ID</th><th>真实姓名</th><th>呼出时间</th><th>呼叫类型</th><th>处理人</th><th>中间号码</th><th>通话状态</th><th>呼叫时长</th><th>通话时长 ↕</th><th>AI分析状态 ◉</th><th>操作</th></tr></thead><tbody>${records.map(call => `<tr><td><button class="call-id-link">${escapeHtml((call.customerId || String(call.id)).replace(/\D/g, "").slice(-10) || String(call.id))}</button></td><td>${escapeHtml(call.customer || "-")}</td><td>${escapeHtml(call.started || "-")}</td><td>${escapeHtml(call.direction || "-")}</td><td>${escapeHtml(call.agent || call.owner || "-")}</td><td>99915400${String(call.id).slice(-1)}</td><td>${call.status === "未接通" ? "客户未接听" : escapeHtml(call.status || "-")}</td><td>${formatDuration(call.durationSeconds || 0)}</td><td>${call.status === "已接通" ? formatDuration(call.durationSeconds || 0) : "00:00"}</td><td>• ${state.callAiPreview ? "分析完成" : "不分析"}</td><td>${call.recording ? "查看录音" : "无录音"}</td></tr>`).join("") || `<tr><td colspan="11" class="call-empty">暂无通话记录</td></tr>`}</tbody></table></div><footer class="call-pagination"><span>1-${records.length} 共${records.length}条</span><button disabled>‹</button><button class="active">1</button><button disabled>›</button><select><option>10 条/页</option></select></footer></section><footer class="call-copyright">版权所有：爱乐云科技有限公司 京ICP备2021032120号</footer>
  </div></section>`;
}

function callNav(active) {
  return subnav(["通话记录", "坐席管理"], active, ["通话记录", "坐席管理"]);
}

function callTasksView() {
  const records = tasks.filter(task => state.callTaskFilter === "all" || (state.callTaskFilter === "pending" && !task.done) || (state.callTaskFilter === "done" && task.done));
  const phoneTasks = records.filter(task => task.type === "电话跟进");
  return `<section class="page">${callNav("通话任务")}<div class="page-content">
    ${pageHeading(`<button class="button secondary" id="exportCallTasks" type="button">${icon("download")}导出任务</button><button class="button primary" id="newCallTask" type="button">${icon("plus")}新建通话任务</button>`)}
    <section class="metric-grid">${metric("通话任务", String(phoneTasks.length), "项", "实时", "blue", "task")}${metric("待处理", String(phoneTasks.filter(task => !task.done).length), "项", "实时", "amber", "clock")}${metric("已完成", String(phoneTasks.filter(task => task.done).length), "项", "实时", "green", "check")}</section>
    <section class="data-panel"><div class="data-toolbar"><div class="data-tabs"><button class="data-tab ${state.callTaskFilter === "all" ? "active" : ""}" data-call-task-filter="all">全部任务</button><button class="data-tab ${state.callTaskFilter === "pending" ? "active" : ""}" data-call-task-filter="pending">待处理</button><button class="data-tab ${state.callTaskFilter === "done" ? "active" : ""}" data-call-task-filter="done">已完成</button></div><span class="pill gray">${phoneTasks.length} 项电话跟进</span></div><div class="table-wrap">${phoneTasks.length ? `<table class="data-table"><thead><tr><th>任务</th><th>客户</th><th>负责人</th><th>截止时间</th><th>优先级</th><th>状态</th><th>操作</th></tr></thead><tbody>${phoneTasks.map(task => `<tr data-task-id="${task.id}"><td><div class="task-name">${escapeHtml(task.title)}</div></td><td>${escapeHtml(task.customer)}</td><td>${escapeHtml(task.owner)}</td><td>${escapeHtml(task.due)}</td><td><span class="pill ${task.priority === "紧急" ? "red" : task.priority === "高" ? "amber" : "gray"}">${escapeHtml(task.priority)}</span></td><td><span class="pill ${task.done ? "green" : task.status === "overdue" ? "red" : "blue"}">${task.done ? "已完成" : task.status === "overdue" ? "已逾期" : "待处理"}</span></td><td><div class="table-actions"><button class="task-check" type="button" aria-label="${task.done ? "标记未完成" : "标记完成"}">${icon("check")}</button><button class="button ghost" type="button" data-task-card="${task.id}">编辑</button></div></td></tr>`).join("")}</tbody></table>` : `<div class="empty-state"><span class="empty-icon">${icon("phone")}</span><strong>暂无通话任务</strong><p>新建电话跟进任务后，会在这里统一安排和完成。</p><button class="button primary" type="button" id="newCallTaskEmpty">${icon("plus")}新建通话任务</button></div>`}</div></section>
  </div></section>`;
}

function callAgentsView() {
  const agents = [...new Set([...calls.map(call => call.agent || call.owner), ...tasks.map(task => task.owner)].filter(Boolean))].sort((a, b) => a.localeCompare(b, "zh-CN"));
  const rows = agents.map(agent => {
    const agentCalls = calls.filter(call => (call.agent || call.owner) === agent);
    const agentTasks = tasks.filter(task => task.owner === agent);
    const connected = agentCalls.filter(call => call.status === "已接通").length;
    return { agent, calls: agentCalls.length, connected, duration: agentCalls.reduce((sum, call) => sum + call.durationSeconds, 0), tasks: agentTasks.length, completed: agentTasks.filter(task => task.done).length };
  });
  return `<section class="page customer-list-page seat-management-page"><div class="page-content"><div class="customer-reference-tabs"><button class="active" type="button">通话监听</button><button type="button">谈话指导</button></div>
    <section class="seat-filter"><label><b>坐席状态：</b><select id="seatStatusFilter"><option>坐席状态</option><option>在线</option><option>离线</option></select></label><label><b>通话人员：</b><select id="seatAgentFilter"><option>请选择</option>${agents.map(agent => `<option>${escapeHtml(agent)}</option>`).join("")}</select></label><div><button class="button primary" id="querySeatAgents" type="button">${icon("search")}查询</button><button class="button secondary" id="resetSeatAgents" type="button">${icon("repeat")}重置</button></div></section>
    <section class="seat-table-panel"><div class="table-wrap"><table class="seat-management-table"><thead><tr><th>通话人员</th><th>坐席状态</th><th>呼叫ID</th><th>呼叫姓名</th><th>客户状态</th><th>通话时长</th><th>空闲时长</th><th>操作</th></tr></thead><tbody>${rows.map(row => `<tr><td>${escapeHtml(row.agent)}</td><td>离线</td><td>${row.calls ? `CALL-${String(row.calls).padStart(4,"0")}` : ""}</td><td>${row.calls ? escapeHtml(row.agent) : ""}</td><td>${row.calls ? "通话结束" : ""}</td><td>${formatDuration(row.duration)}</td><td>${row.calls ? "00:00:00" : "00:00:00"}</td><td><button class="seat-monitor-button" type="button" data-seat-monitor="${escapeHtml(row.agent)}">监听</button></td></tr>`).join("") || `<tr><td colspan="8" class="seat-empty">暂无坐席数据</td></tr>`}</tbody></table></div><footer class="seat-pagination"><span>1-${rows.length} 共${rows.length}条</span><button disabled>‹</button><button class="active">1</button><button disabled>›</button><select><option>10 条/页</option></select></footer></section>
  </div></section>`;
}

async function toggleCallReview(callId) {
  const reviewed = !state.callReviews[callId];
  try {
    requireBackend();
    const result = await apiRequest(`/call-reviews/${encodeURIComponent(callId)}`, { method: "PATCH", body: JSON.stringify({ reviewed }) });
    state.callReviews[callId] = Boolean(result.reviewed);
    localStorage.setItem("youke.crm.callReviews", JSON.stringify(state.callReviews));
    render();
    toast(state.callReviews[callId] ? "已标记为质检完成并写入 MySQL" : "已恢复为待质检");
  } catch (error) {
    toast(`质检状态保存失败：${error.message}`);
  }
}

function callQualityView() {
  const pending = calls.filter(call => !state.callReviews[call.id]);
  const reviewed = calls.length - pending.length;
  return `<section class="page">${callNav("录音质检")}<div class="page-content">
    ${pageHeading(`<button class="button secondary" id="exportCallQuality" type="button">${icon("download")}导出质检结果</button>`)}
    <section class="metric-grid">${metric("待质检", String(pending.length), "条", "实时", "amber", "clock")}${metric("已完成", String(reviewed), "条", "实时", "green", "check")}${metric("质检完成率", calls.length ? `${Math.round(reviewed / calls.length * 100)}` : "0", "%", "实时", "blue", "target")}</section>
    <section class="data-panel"><div class="data-toolbar"><div class="panel-title"><h2>通话录音质检</h2><span>当前通话记录未接入录音文件，先完成结果复核</span></div><span class="pill gray">${calls.length} 条通话</span></div><div class="table-wrap">${calls.length ? `<table class="data-table"><thead><tr><th>客户</th><th>坐席</th><th>通话结果</th><th>时长</th><th>备注</th><th>录音</th><th>质检状态</th><th>操作</th></tr></thead><tbody>${calls.map(call => { const checked = Boolean(state.callReviews[call.id]); return `<tr><td><strong>${escapeHtml(call.customer)}</strong></td><td>${escapeHtml(call.agent || call.owner)}</td><td><span class="pill ${call.status === "已接通" ? "green" : "red"}">${escapeHtml(call.status)}</span></td><td>${formatDuration(call.durationSeconds)}</td><td>${escapeHtml(call.note || "暂无备注")}</td><td><span class="pill gray">暂无录音</span></td><td><span class="pill ${checked ? "green" : "amber"}">${checked ? "已完成" : "待质检"}</span></td><td><button class="button ${checked ? "ghost" : "primary"}" type="button" data-call-review="${call.id}">${checked ? "取消标记" : "完成质检"}</button></td></tr>`; }).join("")}</tbody></table>` : `<div class="empty-state"><span class="empty-icon">${icon("phone")}</span><strong>暂无通话录音</strong><p>完成通话记录后，会在这里进行质检。</p></div>`}</div></section>
  </div></section>`;
}

function callsView() {
  if (state.callSection === "坐席管理") return callAgentsView();
  state.callSection = "通话记录";
  return callRecordsView();
}

function customerMessagesView() {
  const active = conversations.find(item => item.id === state.activeConversationId) || conversations[0];
  const messages = active ? (conversationMessages.get(active.id) || []) : [];
  return customerMessagesInteractiveView(active, messages);
  const messageDates = ["2025-08-19", "2025-08-18", "2025-08-18", "2025-08-18"];
  return `<section class="page message-reference-page">${subnav(["消息管理", "短信记录"], "消息管理", ["消息管理", "短信记录"])}<div class="message-reference-content"><section class="message-reference-layout"><aside class="message-reference-list"><h2>消息</h2><span class="message-count">共 ${conversations.length + 1} 个会话</span><div class="message-reference-tabs"><button class="active">尚未阅读</button><button>客户回复</button></div><div class="message-reference-items"><article class="message-reference-item active"><span class="system-message-icon">${icon("bell")}</span><div><strong>系统消息</strong><time>星期六 10:38</time><p>修改需求提醒</p></div></article>${conversations.map((conversation, index) => `<article class="message-reference-item" data-conversation="${conversation.id}"><span class="person-avatar" style="background:${conversation.color}">${escapeHtml(conversation.name[0])}</span><div><strong>${escapeHtml(conversation.name.replace(/(.{1,3}).*/, "$1****"))}</strong><time>${messageDates[index] || "2025-08-18"}</time><p>${escapeHtml(conversation.preview)}</p></div></article>`).join("")}</div></aside><div class="message-reference-empty"></div></section></div></section>`;
  /* Existing CRM conversation layout retained below for reference. */
  /* return `<section class="page">${subnav(["消息管理", "短信记录"], "消息管理", ["消息管理", "短信记录"])}<div class="page-content">
    ${pageHeading(`<button class="button secondary" id="markAllRead" type="button">${icon("check")}全部已读</button><button class="button primary" id="newConversation" type="button">${icon("plus")}新建会话</button>`)}
    <section class="message-layout"><aside class="conversation-list"><div class="conversation-search"><label class="global-search">${icon("search")}<input id="conversationSearch" placeholder="搜索会话"></label></div>${conversations.map(conversation => `<article class="conversation-item ${conversation.id === active?.id ? "active" : ""}" data-conversation="${conversation.id}"><span class="person-avatar" style="background:${conversation.color}">${escapeHtml(conversation.name[0])}</span><div class="conversation-copy"><strong>${escapeHtml(conversation.name)}</strong><span>${escapeHtml(conversation.preview)}</span></div><div class="conversation-meta"><time>${escapeHtml(conversation.time)}</time>${conversation.unread ? `<span class="unread">${conversation.unread}</span>` : ""}</div></article>`).join("")}</aside>
    <div class="chat-pane">${active ? `<header class="chat-header"><div><h2>${escapeHtml(active.name)} <span class="pill blue">重点客户</span></h2><p>在线 · ${escapeHtml(active.company)}</p></div><div class="inline-actions"><button class="icon-button" data-call-name="${escapeHtml(active.name)}" aria-label="记录通话">${icon("phone")}</button><button class="icon-button" data-open-customer="${escapeHtml(active.customerId)}" aria-label="客户详情">${icon("users")}</button></div></header><div class="messages" id="messageThread">${messages.map(message => `<div class="message ${message.outgoing ? "outgoing" : ""}"><span class="person-avatar">${escapeHtml(message.sender?.[0] || "客")}</span><div class="message-bubble">${escapeHtml(message.content)}<time>${escapeHtml(message.time)}</time></div></div>`).join("") || `<div class="empty-state">暂无消息，发送第一条消息开始沟通</div>`}</div><form class="composer" id="messageForm"><textarea id="messageInput" placeholder="输入消息内容，Enter 发送"></textarea><button class="button primary" type="submit">发送</button></form>` : `<div class="empty-state"><strong>还没有会话</strong><p>点击“新建会话”选择客户</p></div>`}</div></section>
  </div></section>`; */
}

function customerMessagesInteractiveView(active, messages) {
  return `<section class="page">${subnav(["消息管理", "短信记录"], "消息管理", ["消息管理", "短信记录"])}<div class="page-content">
    ${pageHeading(`<button class="button secondary" id="markAllRead" type="button">${icon("check")}全部已读</button><button class="button primary" id="newConversation" type="button">${icon("plus")}新建会话</button>`)}
    <section class="message-layout"><aside class="conversation-list"><div class="conversation-search"><label class="global-search">${icon("search")}<input id="conversationSearch" placeholder="搜索会话"></label></div>${conversations.map(conversation => `<article class="conversation-item ${conversation.id === active?.id ? "active" : ""}" data-conversation="${conversation.id}"><span class="person-avatar">${escapeHtml(conversation.name?.[0] || "客")}</span><div class="conversation-copy"><strong>${escapeHtml(conversation.name)}</strong><span>${escapeHtml(conversation.preview || "")}</span></div><div class="conversation-meta"><time>${escapeHtml(conversation.time || "")}</time>${conversation.unread ? `<span class="unread">${conversation.unread}</span>` : ""}</div></article>`).join("")}</aside><div class="chat-pane">${active ? `<header class="chat-header"><h2>${escapeHtml(active.name)}</h2><p>${escapeHtml(active.company || "")}</p></header><div class="messages" id="messageThread">${messages.map(message => `<div class="message ${message.outgoing ? "outgoing" : ""}"><span class="person-avatar">${escapeHtml(message.sender?.[0] || "客")}</span><div class="message-bubble">${escapeHtml(message.content)}<time>${escapeHtml(message.time || "")}</time></div></div>`).join("") || `<div class="empty-state">暂无消息，发送第一条消息开始沟通</div>`}</div><form class="composer" id="messageForm"><textarea id="messageInput" placeholder="输入消息内容，Enter 发送"></textarea><button class="button primary" type="submit">发送</button></form>` : `<div class="empty-state">还没有会话</div>`}</div></section></div></section>`;
}

function systemNotificationRows() {
  const rows = [];
  tasks.filter(task => !task.done).forEach(task => rows.push({ id: `task-${task.id}`, type: "任务提醒", title: "跟进任务待处理", detail: `${task.title} · ${task.customer}`, owner: task.owner, at: task.dueAt || new Date().toISOString(), tone: "amber", taskId: task.id }));
  conversations.filter(conversation => conversation.unread > 0).forEach(conversation => rows.push({ id: `conversation-${conversation.id}`, type: "客户消息", title: "有未读客户消息", detail: `${conversation.name}：${conversation.preview}`, owner: conversation.owner || "客户顾问", at: conversation.lastMessageAt || new Date().toISOString(), tone: "blue", conversationId: conversation.id }));
  calls.filter(call => ["未接通", "待回拨"].includes(call.status)).forEach(call => rows.push({ id: `call-${call.id}`, type: "通话提醒", title: call.status === "待回拨" ? "客户等待回拨" : "通话未接通", detail: `${call.customer} · ${call.phone}`, owner: call.agent || call.owner, at: call.startedAt || new Date().toISOString(), tone: "red", customerId: call.customerId }));
  if (state.poolCustomers.length) rows.push({ id: "pool-customers", type: "公海提醒", title: "公海有待领取客户", detail: `当前有 ${state.poolCustomers.length} 位客户等待领取`, owner: "系统", at: new Date().toISOString(), tone: "green" });
  return rows.sort((a, b) => new Date(b.at || 0) - new Date(a.at || 0));
}

function updateNotificationChrome() {
  const popover = document.querySelector("#notificationPopover");
  const dot = document.querySelector(".notification-button span");
  if (!popover || !dot) return;
  const rows = systemNotificationRows();
  const unread = rows.filter(row => !state.notificationRead[row.id]);
  dot.hidden = unread.length === 0;
  popover.innerHTML = `<div class="popover-title"><span>最新通知${unread.length ? ` · ${unread.length} 条未读` : ""}</span><button type="button" id="readNotifications" ${unread.length ? "" : "disabled"}>全部已读</button></div>${rows.slice(0, 4).map(row => `<button class="notification-item ${state.notificationRead[row.id] ? "read" : ""}" type="button" data-popover-notification="${escapeHtml(row.id)}"><span class="notification-icon">${icon(row.taskId ? "task" : row.conversationId ? "message" : row.customerId ? "phone" : "users")}</span><div><p>${escapeHtml(row.title)}</p><time>${escapeHtml(row.detail)} · ${escapeHtml(formatRelativeDate(row.at))}</time></div></button>`).join("") || `<div class="notification-empty">暂无待处理通知</div>`}<button class="popover-more" type="button" data-open-notification-center>查看全部通知</button>`;
}

async function persistNotificationRead(ids, read) {
  if (!ids.length) return;
  requireBackend();
  await apiRequest("/notifications/read", {
    method: "PATCH",
    body: JSON.stringify({ notificationIds: ids, read })
  });
}

async function markSystemNotification(id) {
  try {
    await persistNotificationRead([id], true);
    state.notificationRead[id] = true;
    localStorage.setItem("youke.crm.notificationRead", JSON.stringify(state.notificationRead));
    render();
  } catch (error) { toast(`通知状态保存失败：${error.message}`); }
}

async function markAllSystemNotificationsRead() {
  const ids = systemNotificationRows().map(row => row.id);
  try {
    await persistNotificationRead(ids, true);
    ids.forEach(id => { state.notificationRead[id] = true; });
    localStorage.setItem("youke.crm.notificationRead", JSON.stringify(state.notificationRead));
    render();
    toast("系统通知已全部标记为已读");
  } catch (error) { toast(`通知状态保存失败：${error.message}`); }
}

async function openSystemNotification(row) {
  await markSystemNotification(row.id);
  if (row.taskId) {
    const task = tasks.find(item => String(item.id) === String(row.taskId));
    if (task) openBusinessModal("task", task);
  } else if (row.conversationId) {
    state.activeConversationId = Number(row.conversationId);
    navigate("messages");
    await selectConversation(row.conversationId);
  } else if (row.customerId) {
    openCustomer(row.customerId);
  }
}

function exportSystemNotifications() {
  const rows = systemNotificationRows();
  const header = ["类型", "标题", "通知内容", "负责人", "时间", "状态"];
  const csv = "\ufeff" + [header, ...rows.map(row => [row.type, row.title, row.detail, row.owner, formatRelativeDate(row.at), state.notificationRead[row.id] ? "已读" : "未读"])]
    .map(row => row.map(value => `"${String(value ?? "").replaceAll('"', '""')}"`).join(",")).join("\n");
  const link = document.createElement("a"); link.href = URL.createObjectURL(new Blob([csv], { type: "text/csv;charset=utf-8" })); link.download = "优客云-系统通知.csv"; link.click(); setTimeout(() => URL.revokeObjectURL(link.href), 0);
  toast(`已导出 ${rows.length} 条系统通知`);
}

function systemNotificationsView() {
  const allRows = systemNotificationRows();
  const unread = allRows.filter(row => !state.notificationRead[row.id]);
  const rows = allRows.filter(row => state.notificationFilter === "all" || (state.notificationFilter === "unread" && !state.notificationRead[row.id]) || (state.notificationFilter === "read" && state.notificationRead[row.id]));
  return `<section class="page">${subnav(["消息管理", "短信记录"], "短信记录", ["消息管理", "短信记录"])}<div class="page-content">
    ${pageHeading(`<button class="button secondary" id="exportSystemNotifications" type="button">${icon("download")}导出通知</button><button class="button primary" id="markAllSystemNotifications" type="button">${icon("check")}全部已读</button>`)}
    <section class="metric-grid">${metric("通知总数", String(allRows.length), "条", "实时", "blue", "bell")}${metric("未读通知", String(unread.length), "条", "实时", "amber", "target")}${metric("已读通知", String(allRows.length - unread.length), "条", "实时", "green", "check")}</section>
    <section class="data-panel notification-panel"><div class="data-toolbar"><div class="data-tabs"><button class="data-tab ${state.notificationFilter === "all" ? "active" : ""}" data-notification-filter="all">全部 <span class="count">${allRows.length}</span></button><button class="data-tab ${state.notificationFilter === "unread" ? "active" : ""}" data-notification-filter="unread">未读 <span class="count">${unread.length}</span></button><button class="data-tab ${state.notificationFilter === "read" ? "active" : ""}" data-notification-filter="read">已读</button></div><span class="pill gray">${rows.length} 条记录</span></div><div class="system-notification-list">${rows.length ? rows.map(row => { const read = Boolean(state.notificationRead[row.id]); return `<article class="system-notification ${read ? "read" : "unread"}" data-system-notification="${escapeHtml(row.id)}"><span class="notification-icon ${row.tone}">${icon(row.type === "任务提醒" ? "task" : row.type === "客户消息" ? "message" : row.type === "通话提醒" ? "phone" : "users")}</span><div class="system-notification-body"><div class="system-notification-top"><strong>${escapeHtml(row.title)}</strong><time>${escapeHtml(formatRelativeDate(row.at))}</time></div><p>${escapeHtml(row.detail)}</p><small>${escapeHtml(row.type)} · ${escapeHtml(row.owner || "系统")}</small></div><div class="system-notification-actions">${read ? `<span class="pill gray">已读</span>` : `<span class="pill amber">未读</span>`}<button class="button ghost" type="button" data-open-notification="${escapeHtml(row.id)}">${row.taskId ? "查看任务" : row.conversationId ? "查看消息" : row.customerId ? "查看客户" : "标记已读"}</button></div></article>`; }).join("") : `<div class="empty-state"><span class="empty-icon">${icon("bell")}</span><strong>暂无系统通知</strong><p>新的任务、未读消息和回拨提醒会显示在这里。</p></div>`}</div></section>
  </div></section>`;
}

function saveMessageTemplates() {
  localStorage.setItem("youke.crm.messageTemplates", JSON.stringify(state.messageTemplates));
}

function messageTemplatesView() {
  const editing = state.messageTemplates.find(template => template.id === state.editingTemplateId);
  const search = String(state.messageTemplateSearch || "").trim().toLowerCase();
  const templates = state.messageTemplates.filter(template => !search || `${template.name} ${template.content} ${template.channel}`.toLowerCase().includes(search));
  return `<section class="page">${subnav(["消息管理", "短信记录"], "短信记录", ["消息管理", "短信记录"])}<div class="page-content">
    ${pageHeading(`<button class="button secondary" id="resetMessageTemplate" type="button">${icon("plus")}新建模板</button>`)}
    <section class="data-panel template-editor"><div class="data-toolbar"><div class="panel-title"><h2>${editing ? "编辑消息模板" : "新增消息模板"}</h2><span>模板可带入会话输入框，再确认后发送</span></div></div><form id="messageTemplateForm" class="message-template-form"><input type="hidden" name="templateId" value="${escapeHtml(editing?.id || "")}"><label><span>模板名称 *</span><input name="name" required maxlength="64" value="${escapeHtml(editing?.name || "")}" placeholder="例如：报价跟进"></label><label><span>发送渠道</span><select name="channel">${["微信 / 短信", "电话后跟进", "邮件"].map(channel => `<option ${channel === (editing?.channel || "微信 / 短信") ? "selected" : ""}>${channel}</option>`).join("")}</select></label><label class="form-span-2"><span>模板内容 *</span><textarea name="content" required maxlength="2000" rows="4" placeholder="输入常用的客户沟通内容">${escapeHtml(editing?.content || "")}</textarea></label><div class="form-span-2 inline-actions"><button class="button primary" type="submit">${icon("check")}${editing ? "保存修改" : "保存模板"}</button>${editing ? `<button class="button secondary" type="button" id="cancelMessageTemplateEdit">取消编辑</button>` : ""}</div></form></section>
    <section class="data-panel"><div class="data-toolbar"><div class="panel-title"><h2>模板列表</h2><span>${state.messageTemplates.length} 个模板</span></div><label class="compact-filter"><span>搜索</span><input id="messageTemplateSearch" value="${escapeHtml(state.messageTemplateSearch || "")}" placeholder="名称或内容"></label></div><div class="template-use-bar"><label><span>选择客户</span><select id="templateCustomer">${customers.map(customer => `<option value="${escapeHtml(customer.id)}">${escapeHtml(customer.name)} · ${escapeHtml(customer.company)}</option>`).join("")}</select></label><span>点击“使用模板”后会打开客户会话并填入内容</span></div><div class="template-list">${templates.length ? templates.map(template => { const canManage = isAdmin() || template.ownerUsername !== "*"; return `<article class="message-template-card"><div class="template-card-main"><div class="template-card-title"><strong>${escapeHtml(template.name)}</strong><span class="pill blue">${escapeHtml(template.channel)}</span></div><p>${escapeHtml(template.content)}</p><small>更新：${escapeHtml(template.updatedAt || "刚刚")}${template.ownerUsername === "*" ? " · 系统预置" : ""}</small></div><div class="template-card-actions"><button class="button primary" type="button" data-use-message-template="${escapeHtml(template.id)}">${icon("message")}使用模板</button>${canManage ? `<button class="button ghost" type="button" data-edit-message-template="${escapeHtml(template.id)}">编辑</button><button class="button danger" type="button" data-delete-message-template="${escapeHtml(template.id)}">删除</button>` : `<span class="pill gray">只读模板</span>`}</div></article>`; }).join("") : `<div class="empty-state"><span class="empty-icon">${icon("message")}</span><strong>没有匹配的消息模板</strong><p>调整搜索条件或新建一个模板。</p></div>`}</div></section>
  </div></section>`;
}

function messagesView() {
  if (state.messageSection === "短信记录") return smsRecordsView();
  return customerMessagesView();
}

function smsRecordsView() {
  return `<section class="page sms-reference-page">${subnav(["消息管理", "短信记录"], "短信记录", ["消息管理", "短信记录"])}<div class="sms-reference-content"><section class="sms-filter-panel"><div class="sms-filter-row"><label>收发状态：<select><option>全部</option></select></label><label>收发时间：<input value="2026-08-26 00:00　—　2026-08-26 23:59"></label><label>所属人：<input placeholder="请选择所属人"></label><button class="button primary" type="button">${icon("search")}查询</button><button class="button secondary" type="button">${icon("repeat")}重置</button></div></section><section class="data-panel sms-reference-panel"><div class="table-wrap"><table class="data-table sms-reference-table"><thead><tr><th>id</th><th>会员姓名</th><th>会员ID</th><th>短信类型</th><th>员工姓名</th><th>短信内容</th><th>短信长度</th><th>收发时间</th><th>状态</th></tr></thead><tbody><tr><td colspan="9"><div class="empty-state"><span class="empty-icon">${icon("message")}</span><strong>暂无数据</strong></div></td></tr></tbody></table></div></section></div></section>`;
}

async function submitMessageTemplate(event) {
  event.preventDefault();
  const data = Object.fromEntries(new FormData(event.currentTarget));
  const name = String(data.name || "").trim();
  const content = String(data.content || "").trim();
  if (!name || !content) return;
  const existing = state.messageTemplates.find(template => template.id === data.templateId);
  try {
    requireBackend();
    const payload = { name, content, channel: data.channel };
    const saved = await apiRequest(existing ? `/message-templates/${encodeURIComponent(existing.id)}` : "/message-templates", { method: existing ? "PUT" : "POST", body: JSON.stringify(payload) });
    const normalized = normalizeMessageTemplate(saved);
    if (existing) state.messageTemplates = state.messageTemplates.map(template => template.id === normalized.id ? normalized : template);
    else state.messageTemplates = [normalized, ...state.messageTemplates];
    saveMessageTemplates();
    state.editingTemplateId = null;
    render();
    toast(existing ? "消息模板已更新" : "消息模板已创建");
  } catch (error) {
    toast(`消息模板保存失败：${error.message}`);
  }
}

function editMessageTemplate(id) {
  state.editingTemplateId = id;
  render();
  document.querySelector("#messageTemplateForm input[name=name]")?.focus();
}

async function deleteMessageTemplate(id) {
  const template = state.messageTemplates.find(item => item.id === id);
  if (!template || !window.confirm(`确定删除消息模板“${template.name}”吗？`)) return;
  try {
    requireBackend();
    await apiRequest(`/message-templates/${encodeURIComponent(id)}`, { method: "DELETE" });
    state.messageTemplates = state.messageTemplates.filter(item => item.id !== id);
    if (state.editingTemplateId === id) state.editingTemplateId = null;
    saveMessageTemplates();
    render();
    toast("消息模板已删除");
  } catch (error) {
    toast(`消息模板删除失败：${error.message}`);
  }
}

async function useMessageTemplate(id) {
  const template = state.messageTemplates.find(item => item.id === id);
  const customerId = document.querySelector("#templateCustomer")?.value;
  const customer = customers.find(item => item.id === customerId);
  if (!template || !customer) { toast("请先选择客户"); return; }
  await openConversationForCustomer(customer.name);
  const input = document.querySelector("#messageInput");
  if (input) { input.value = template.content; input.focus(); }
  toast(`已将“${template.name}”带入${customer.name}的会话`);
}

function orderPill(status) {
  const tones = { "已支付": "green", "部分支付": "amber", "待支付": "blue", "已取消": "red" };
  return `<span class="pill ${tones[status] || "gray"}">${status}</span>`;
}

function filteredOrders() {
  const keyword = state.orderKeyword.trim().toLowerCase();
  return orders.filter(order => {
    const keywordMatch = !keyword || [order.id, order.customer, order.product].join(" ").toLowerCase().includes(keyword);
    const paymentMatch = state.orderPaymentStatus === "全部状态" || order.status === state.orderPaymentStatus;
    const serviceMatch = state.orderServiceStatus === "全部状态" || order.service === state.orderServiceStatus;
    return keywordMatch && paymentMatch && serviceMatch;
  });
}

function orderNav(active) {
  const sections = ["订单列表", "合同列表", "流水列表", "业绩上传", "退费列表", "业绩列表", "卡券管理"];
  return subnav(sections, active, sections);
}

function orderStatusClass(status) {
  return status === "已支付" || status === "已确认" ? "green" : status === "部分支付" || status === "处理中" ? "amber" : status === "已取消" || status === "已拒绝" ? "red" : "blue";
}

function orderRowsForSection() {
  return orders.filter(order => !state.orderKeyword.trim() || `${order.id} ${order.customer} ${order.product}`.toLowerCase().includes(state.orderKeyword.trim().toLowerCase()));
}

function orderListView() {
  const rows = filteredOrders();
  const pageSize = 10;
  const totalPages = Math.max(1, Math.ceil(rows.length / pageSize));
  const currentPage = Math.min(Math.max(1, state.orderPage || 1), totalPages);
  if (currentPage !== state.orderPage) state.orderPage = currentPage;
  const pageRows = rows.slice((currentPage - 1) * pageSize, currentPage * pageSize);
  const tableRows = pageRows.map((order, index) => {
    const memberId = `14300${String(33376 - index * 846).padStart(5, "0")}`;
    const invitee = index % 3 === 1 ? "管理员" : "赵娜";
    return `<tr data-order-id="${escapeHtml(order.id)}"><td><button class="table-link legacy-order-id">${escapeHtml(order.id.replace(/^SO2026/, "27"))}</button></td><td>优爱天津店</td><td><button class="table-link">${memberId}</button></td><td>${escapeHtml(order.customer)}</td><td>线下VIP</td><td>${order.amount}/${order.paid}</td><td>${orderPill(order.status)}</td><td>${escapeHtml(order.service === "待开通" ? "待服务" : order.service)}</td><td>${escapeHtml(order.created)}</td><td>${invitee}</td><td>${escapeHtml(order.owner)}</td><td>—</td><td><button class="button ghost order-detail-button" type="button" data-order-detail="${escapeHtml(order.id)}">详情</button></td></tr>`;
  }).join("");
  const pageButtons = `<button class="page-button" type="button" data-order-page="${currentPage - 1}" ${currentPage === 1 ? "disabled" : ""}>‹</button>${Array.from({ length: totalPages }, (_, index) => `<button class="page-button ${index + 1 === currentPage ? "active" : ""}" type="button" data-order-page="${index + 1}">${index + 1}</button>`).join("")}<button class="page-button" type="button" data-order-page="${currentPage + 1}" ${currentPage === totalPages ? "disabled" : ""}>›</button>`;
  const rangeStart = rows.length ? (currentPage - 1) * pageSize + 1 : 0;
  const rangeEnd = Math.min(currentPage * pageSize, rows.length);
  return `<section class="page order-list-reference">${orderNav("订单列表")}<div class="order-reference-content"><section class="data-panel order-reference-panel"><div class="table-wrap">${rows.length ? `<table class="data-table order-reference-table"><thead><tr><th>订单ID</th><th>所属门店</th><th>会员ID</th><th>会员姓名</th><th>套餐</th><th>应付/实付金额</th><th>订单状态</th><th>服务状态</th><th>创建时间</th><th>邀约</th><th>销售</th><th>共同单人</th><th>操作</th></tr></thead><tbody>${tableRows}</tbody></table>` : `<div class="empty-state"><span class="empty-icon">${icon("search")}</span><strong>暂无订单</strong><p>当前没有可展示的订单。</p></div>`}</div><footer class="order-reference-pagination"><span>${rangeStart}-${rangeEnd} 共${rows.length}条</span>${pageButtons}<select aria-label="每页条数"><option>10 条/页</option></select>${totalPages > 1 ? `<span>跳至</span><input aria-label="跳转页码"><span>页</span>` : ""}</footer></section></div></section>`;
  /* legacy layout retained below for reference */
  /* return `<section class="page">${orderNav("订单列表")}<div class="page-content">
    ${pageHeading(`<button class="button secondary" id="exportOrders">${icon("download")}导出订单</button><button class="button primary" id="newOrder">${icon("plus")}新建订单</button>`)}
    <section class="filter-panel"><div class="filter-row"><label class="field"><span>订单 / 客户</span><input id="orderKeyword" value="${escapeHtml(state.orderKeyword)}" placeholder="订单号或客户姓名"></label><label class="field"><span>支付状态</span><select id="orderPaymentFilter">${["全部状态","待支付","部分支付","已支付","已取消"].map(option => `<option ${option === state.orderPaymentStatus ? "selected" : ""}>${option}</option>`).join("")}</select></label><label class="field"><span>服务状态</span><select id="orderServiceFilter">${["全部状态","未开始","待开通","实施中","已开通"].map(option => `<option ${option === state.orderServiceStatus ? "selected" : ""}>${option}</option>`).join("")}</select></label></div><div class="filter-footer"><span class="pill gray">筛选订单金额 ${money(totalAmount)}</span><div class="inline-actions"><button class="button secondary" id="resetOrderFilters">重置</button><button class="button primary" id="applyOrderFilters">${icon("search")}查询</button></div></div></section>
    <section class="data-panel"><div class="data-toolbar"><div class="data-tabs"><button class="data-tab active">全部订单 <span class="count">${rows.length}</span></button></div><span class="pill green">筛选回款 ${money(paidAmount)}</span></div><div class="table-wrap">${rows.length ? `<table class="data-table"><thead><tr><th>订单号</th><th>客户</th><th>商品 / 套餐</th><th>订单金额</th><th>已付金额</th><th>支付状态</th><th>服务状态</th><th>销售</th><th>创建时间</th><th>操作</th></tr></thead><tbody>${rows.map(order => `<tr data-order-id="${order.id}"><td><button class="table-link">${order.id}</button></td><td>${order.customer}</td><td>${order.product}</td><td><strong>${money(order.amount)}</strong></td><td>${money(order.paid)}</td><td>${orderPill(order.status)}</td><td><span class="pill ${order.service === "已开通" ? "green" : order.service === "实施中" ? "purple" : "gray"}">${order.service}</span></td><td>${order.owner}</td><td>${order.created}</td><td><button class="button ghost" data-order-detail="${order.id}">详情${icon("chevron")}</button></td></tr>`).join("")}</tbody></table>` : `<div class="empty-state"><span class="empty-icon">${icon("search")}</span><strong>没有匹配的订单</strong><p>调整筛选条件后再试一次</p></div>`}</div><footer class="pagination"><span>共 ${rows.length} 条</span><button class="page-button active">1</button><span>20 条/页</span></footer></section>
  </div></section>`; */
}

function contractListView() {
  const rows = orderRowsForSection();
  const contractRows = rows.map((order, index) => `<tr data-order-id="${escapeHtml(order.id)}"><td><button class="table-link legacy-order-id">${escapeHtml(order.id.replace(/^SO2026/, "27"))}</button></td><td><button class="table-link">14300${String(37960 - index * 430).padStart(5, "0")}</button></td><td>${escapeHtml(order.created)}</td><td>EYA2026082320${String(1063 - index * 77).padStart(4, "0")}</td><td><span class="pill green">审核通过</span></td><td>${escapeHtml(order.customer)}</td><td>${escapeHtml(order.owner)}</td><td><button class="button ghost order-detail-button" type="button" data-order-detail="${escapeHtml(order.id)}">详情</button></td></tr>`).join("");
  return `<section class="page order-contract-reference">${orderNav("合同列表")}<div class="order-contract-content"><section class="contract-filter-panel"><div class="contract-filter-row"><label>会员ID<input placeholder="请输入用户ID"></label><label>归属人<input placeholder="请选择"></label><label>状态<select><option>请选择</option><option>审核通过</option><option>待审核</option></select></label></div><div class="contract-filter-actions"><button class="button primary" type="button">${icon("search")}查询</button><button class="button secondary" type="button">${icon("repeat")}重置</button></div></section><section class="data-panel order-contract-panel"><div class="table-wrap">${rows.length ? `<table class="data-table order-contract-table"><thead><tr><th>订单ID</th><th>会员ID</th><th>创建时间</th><th>合同编号</th><th>状态</th><th>会员姓名</th><th>归属人</th><th>操作</th></tr></thead><tbody>${contractRows}</tbody></table>` : `<div class="empty-state"><span class="empty-icon">${icon("order")}</span><strong>暂无合同</strong></div>`}</div></section></div></section>`;
  /* Existing CRM contract view retained below for reference. */
  /*
  const active = rows.filter(order => order.status !== "已取消");
  return `<section class="page">${orderNav("合同列表")}<div class="page-content">
    ${pageHeading(`<button class="button secondary" id="exportContracts" type="button">${icon("download")}导出合同</button><button class="button primary" id="newContractOrder" type="button">${icon("plus")}新建合同订单</button>`)}
    <section class="metric-grid">${metric("合同总数", String(active.length), "份", "实时", "blue", "order")}${metric("合同金额", money(active.reduce((sum, order) => sum + order.amount, 0)), "", "实时", "purple", "wallet")}${metric("待回款", money(active.reduce((sum, order) => sum + order.amount - order.paid, 0)), "", "实时", "amber", "clock")}</section>
    <section class="data-panel"><div class="data-toolbar"><div class="panel-title"><h2>合同列表</h2><span>订单即合同，展示合同金额、回款和履约状态</span></div><span class="pill gray">${rows.length} 份</span></div><div class="table-wrap">${rows.length ? `<table class="data-table"><thead><tr><th>合同编号</th><th>客户</th><th>产品 / 套餐</th><th>合同金额</th><th>已回款</th><th>回款状态</th><th>履约状态</th><th>销售</th><th>创建时间</th><th>操作</th></tr></thead><tbody>${rows.map(order => `<tr data-order-id="${escapeHtml(order.id)}"><td><button class="table-link">${escapeHtml(order.id)}</button></td><td>${escapeHtml(order.customer)}</td><td>${escapeHtml(order.product)}</td><td><strong>${money(order.amount)}</strong></td><td>${money(order.paid)}</td><td>${orderPill(order.status)}</td><td><span class="pill ${order.service === "已开通" ? "green" : order.service === "实施中" ? "purple" : "gray"}">${escapeHtml(order.service)}</span></td><td>${escapeHtml(order.owner)}</td><td>${escapeHtml(order.created)}</td><td><button class="button ghost" type="button" data-order-detail="${escapeHtml(order.id)}">详情${icon("chevron")}</button></td></tr>`).join("")}</tbody></table>` : `<div class="empty-state"><span class="empty-icon">${icon("order")}</span><strong>暂无合同</strong><p>新建订单后会自动生成合同记录。</p></div>`}</div></section>
  </div></section>`; */
}

function paymentRecordsView() {
  const rows = orderRowsForSection().filter(order => order.paid > 0);
  const totalPaid = rows.reduce((sum, order) => sum + order.paid, 0);
  const filter = (label, placeholder) => `<label>${label}<input placeholder="${placeholder}"></label>`;
  const select = label => `<label>${label}<select><option>请选择</option></select></label>`;
  const paymentRows = rows.map((order, index) => `<tr data-order-id="${escapeHtml(order.id)}"><td>PAY-${String(index + 1).padStart(4, "0")}</td><td>${escapeHtml(order.id)}</td><td>优爱天津店</td><td>${escapeHtml(order.customer)}</td><td>${escapeHtml(order.product)}</td><td>${money(order.paid)}</td><td>线下支付</td><td>${escapeHtml(order.created)}</td><td>${escapeHtml(order.created)}</td><td>审核通过</td><td>${orderPill(order.status)}</td><td><button class="button ghost order-detail-button" type="button" data-order-detail="${escapeHtml(order.id)}">详情</button></td></tr>`).join("");
  return `<section class="page order-payment-reference">${orderNav("流水列表")}<div class="order-payment-content"><section class="payment-filter-panel"><div class="payment-filter-grid">${filter("流水ID：", "请输入流水ID")}${filter("订单ID：", "请输入订单ID")}${filter("会员ID：", "请输入会员ID")}${select("所属门店：")}${select("提交状态：")}${select("审核状态：")}${select("支付状态：")}${filter("支付时间：", "2026-08-01　—　2026-08-31")}${select("所属人：")}</div><div class="payment-filter-actions"><button class="button primary" type="button">${icon("search")}查询</button><button class="button secondary" type="button">${icon("repeat")}重置</button></div></section><section class="data-panel order-payment-panel"><div class="payment-summary">汇总：${rows.length} 笔流水 共计 ${money(totalPaid)}</div><div class="table-wrap">${rows.length ? `<table class="data-table order-payment-table"><thead><tr><th>支付流水ID</th><th>订单ID</th><th>所属门店</th><th>会员</th><th>产品</th><th>支付金额</th><th>支付方式</th><th>创建时间</th><th>支付时间</th><th>审核状态</th><th>支付状态</th><th>操作</th></tr></thead><tbody>${paymentRows}</tbody></table>` : `<table class="data-table order-payment-table"><thead><tr><th>支付流水ID</th><th>订单ID</th><th>所属门店</th><th>会员</th><th>产品</th><th>支付金额</th><th>支付方式</th><th>创建时间</th><th>支付时间</th><th>审核状态</th><th>支付状态</th><th>操作</th></tr></thead><tbody><tr><td colspan="12"><div class="empty-state"><span class="empty-icon">${icon("wallet")}</span><strong>暂无数据</strong></div></td></tr></tbody></table>`}</div></section></div></section>`;
  /* Existing CRM payment view retained below for reference. */
  /*
  return `<section class="page">${orderNav("流水列表")}<div class="page-content">
    ${pageHeading(`<button class="button secondary" id="exportPayments" type="button">${icon("download")}导出回款</button><button class="button primary" id="newPaymentRecord" type="button">${icon("plus")}登记回款</button>`)}
    <section class="metric-grid">${metric("流水记录", String(rows.length), "笔", "实时", "blue", "wallet")}${metric("累计回款", money(totalPaid), "", "实时", "green", "check")}${metric("待回款订单", String(orderRowsForSection().filter(order => order.amount > order.paid).length), "个", "实时", "amber", "clock")}</section>
    <section class="data-panel"><div class="data-toolbar"><div class="panel-title"><h2>流水列表</h2><span>按订单查看已登记流水，可继续补录金额</span></div><span class="pill gray">${rows.length} 笔</span></div><div class="table-wrap">${rows.length ? `<table class="data-table"><thead><tr><th>流水编号</th><th>订单号</th><th>客户</th><th>订单金额</th><th>本次累计回款</th><th>剩余应收</th><th>支付状态</th><th>操作</th></tr></thead><tbody>${rows.map((order, index) => `<tr><td>PAY-${String(index + 1).padStart(4, "0")}</td><td>${escapeHtml(order.id)}</td><td>${escapeHtml(order.customer)}</td><td>${money(order.amount)}</td><td><strong>${money(order.paid)}</strong></td><td>${money(Math.max(0, order.amount - order.paid))}</td><td>${orderPill(order.status)}</td><td><button class="button ghost" type="button" data-payment-order="${escapeHtml(order.id)}">继续登记</button></td></tr>`).join("")}</tbody></table>` : `<div class="empty-state"><span class="empty-icon">${icon("wallet")}</span><strong>暂无流水记录</strong><p>订单登记回款后会显示在这里。</p><button class="button primary" type="button" id="newPaymentRecordEmpty">${icon("plus")}登记回款</button></div>`}</div></section>
  </div></section>`; */
}

function performanceUploadView() {
  return `<section class="page">${orderNav("业绩上传")}<div class="page-content">
    ${pageHeading(`<button class="button secondary" type="button" id="downloadPerformanceTemplate">${icon("download")}下载模板</button><button class="button primary" type="button" id="uploadPerformanceFile">${icon("plus")}上传业绩</button>`)}
    <section class="data-panel"><div class="data-toolbar"><div class="panel-title"><h2>业绩上传</h2><span>使用标准模板批量导入订单业绩数据</span></div><span class="pill gray">支持 CSV 文件</span></div><div class="empty-state"><span class="empty-icon">${icon("download")}</span><strong>上传业绩文件</strong><p>请先下载模板，按模板填写后上传；系统会校验订单号、销售和回款金额。</p><button class="button primary" type="button" id="uploadPerformanceFileEmpty">${icon("plus")}选择文件</button></div></section>
  </div></section>`;
}

function saveOrderRefunds() { localStorage.setItem("youke.crm.orderRefunds", JSON.stringify(state.orderRefunds)); }

function refundView() {
  const rows = state.orderRefunds.filter(refund => !state.orderKeyword.trim() || `${refund.orderId} ${refund.customer}`.toLowerCase().includes(state.orderKeyword.trim().toLowerCase()));
  const pending = rows.filter(refund => refund.status === "待审核").length;
  const refundRows = rows.map(refund => `<tr><td>1430021483</td><td>${escapeHtml(refund.customer)}</td><td>${escapeHtml(refund.orderId)}</td><td>专业版</td><td>优爱天津店</td><td>赵娜</td><td>¥0</td><td><strong>${money(refund.amount)}</strong></td><td>客户申请</td><td>是</td><td>${escapeHtml(refund.reason)}</td><td>${escapeHtml(refund.applicant)}</td><td>2026-08-23 20:10:03</td><td>${escapeHtml(refund.status)}</td><td>—</td><td>—</td></tr>`).join("");
  return `<section class="page order-refund-reference">${orderNav("退费列表")}<div class="order-refund-content"><section class="refund-filter-panel"><div class="refund-filter-grid"><label>会员ID：<input placeholder="请输入会员ID"></label><label>所属门店：<input placeholder="请选择所属门店"></label><label>涉及员工：<input placeholder="请输入涉及员工"></label><label>退费类型：<input placeholder="请选择退费类型"></label><label>服务是否有消费：<select><option>请选择服务是否有消费</option></select></label><label>发起人：<input placeholder="请输入发起人"></label><label>状态：<select><option>请选择退费状态</option></select></label><label>订单时间：<input placeholder="开始日期　—　结束日期"></label><label>发起时间：<input placeholder="开始日期　—　结束日期"></label><label>完成时间：<input placeholder="开始日期　—　结束日期"></label></div><div class="refund-filter-actions"><button class="button primary" type="button">${icon("search")}查询</button><button class="button secondary" type="button">${icon("repeat")}重置</button></div></section><section class="data-panel order-refund-panel"><div class="refund-summary">已退费${rows.length}笔，总金额${money(rows.reduce((sum, refund) => sum + refund.amount, 0))}</div><div class="table-wrap"><table class="data-table order-refund-table"><thead><tr><th>会员ID</th><th>姓名</th><th>订单ID</th><th>产品套餐</th><th>所属门店</th><th>涉及员工</th><th>实付金额</th><th>退费金额</th><th>退费类型</th><th>服务是否有消费</th><th>退费原因</th><th>发起人</th><th>发起时间</th><th>状态</th><th>完成时间</th><th>操作</th></tr></thead><tbody>${refundRows || `<tr><td colspan="16"><div class="empty-state"><span class="empty-icon">${icon("wallet")}</span><strong>暂无数据</strong></div></td></tr>`}</tbody></table></div></section></div></section>`;
  /* Existing CRM refund view retained below for reference. */
  /*
  return `<section class="page">${orderNav("退费列表")}<div class="page-content">
    ${pageHeading(`<button class="button secondary" id="exportRefunds" type="button">${icon("download")}导出退款</button><button class="button primary" id="newRefund" type="button">${icon("plus")}发起退款</button>`)}
    <section class="metric-grid">${metric("退款申请", String(rows.length), "笔", "实时", "blue", "wallet")}${metric("待审核", String(pending), "笔", "实时", "amber", "clock")}${metric("退款金额", money(rows.reduce((sum, refund) => sum + refund.amount, 0)), "", "实时", "red", "arrow-down")}</section>
    <section class="data-panel"><div class="data-toolbar"><div class="panel-title"><h2>退费列表</h2><span>退费申请先进入待审核，可记录审核结果</span></div><span class="pill gray">${rows.length} 笔</span></div><div class="table-wrap">${rows.length ? `<table class="data-table"><thead><tr><th>退费编号</th><th>订单号</th><th>客户</th><th>退费金额</th><th>退费原因</th><th>申请人</th><th>状态</th><th>操作</th></tr></thead><tbody>${rows.map(refund => `<tr><td>${escapeHtml(refund.id)}</td><td>${escapeHtml(refund.orderId)}</td><td>${escapeHtml(refund.customer)}</td><td><strong>${money(refund.amount)}</strong></td><td>${escapeHtml(refund.reason)}</td><td>${escapeHtml(refund.applicant)}</td><td><span class="pill ${orderStatusClass(refund.status)}">${escapeHtml(refund.status)}</span></td><td>${refund.status === "待审核" && isAdmin() ? `<button class="button ghost" type="button" data-refund-review="${escapeHtml(refund.id)}">审核</button>` : "-"}</td></tr>`).join("")}</tbody></table>` : `<div class="empty-state"><span class="empty-icon">${icon("wallet")}</span><strong>暂无退费申请</strong><p>点击“发起退费”记录客户退费申请。</p></div>`}</div></section>
  </div></section>`; */
}

function performanceView() {
  const rows = orderRowsForSection();
  const pending = rows.filter(order => !order.performanceConfirmed && order.paid > 0);
  const performanceRows = rows.map((order, index) => `<tr><td>${escapeHtml(order.created)}</td><td>29${String(2101 - index).padStart(4, "0")}</td><td><button class="table-link">${escapeHtml(order.id.replace(/^SO2026/, "16"))}</button></td><td><button class="table-link">14300${String(21483 - index * 528).padStart(5, "0")}</button></td><td>${escapeHtml(order.customer)}</td><td>${escapeHtml(order.owner)}</td><td>${order.amount}/${order.paid}</td><td>${index % 2 ? "80%" : "20%"}</td><td>${Math.round(order.paid * (index % 2 ? .8 : .2))}</td><td>${order.performanceConfirmed ? "已确认" : order.paid > 0 ? `<button class="button primary" type="button" data-confirm-performance="${escapeHtml(order.id)}">确认业绩</button>` : "待确认"}</td></tr>`).join("");
  return `<section class="page order-performance-reference">${orderNav("业绩列表")}<div class="order-performance-content"><section class="performance-filter-panel"><div class="performance-filter-grid"><label>订单ID：<input placeholder="请输入订单ID"></label><label>员工姓名：<input placeholder="请选择"></label><label>会员ID：<input placeholder="请输入会员ID"></label><label>状态：<select><option>请选择</option><option>待确认</option><option>已确认</option></select></label><label>创建时间：<input placeholder="开始日期　—　结束日期"></label></div><div class="performance-filter-actions"><button class="button primary" type="button">${icon("search")}查询</button><button class="button secondary" type="button">${icon("repeat")}重置</button></div></section><section class="data-panel order-performance-panel"><div class="performance-summary">总收款业绩：${money(rows.filter(order => order.performanceConfirmed).reduce((sum, order) => sum + order.paid, 0))} <span>（定金、销售后订单不可计入提成）</span></div><div class="table-wrap"><table class="data-table order-performance-table"><thead><tr><th>创建时间</th><th>业绩ID</th><th>订单ID</th><th>会员ID</th><th>会员姓名</th><th>员工姓名</th><th>应付/实付金额</th><th>业绩比例</th><th>业绩</th><th>状态</th></tr></thead><tbody>${performanceRows || `<tr><td colspan="10"><div class="empty-state"><span class="empty-icon">${icon("check")}</span><strong>暂无数据</strong></div></td></tr>`}</tbody></table></div></section></div></section>`;
  /* Existing CRM performance view retained below for reference. */
  /* return `<section class="page">${orderNav("业绩列表")}<div class="page-content">
    ${pageHeading(`<button class="button secondary" id="exportPerformance" type="button">${icon("download")}导出业绩</button><button class="button primary" id="confirmAllPerformance" type="button" ${pending.length ? "" : "disabled"}>${icon("check")}确认全部已付订单</button>`)}
    <section class="metric-grid">${metric("订单总数", String(rows.length), "个", "实时", "blue", "order")}${metric("待确认", String(pending.length), "个", "实时", "amber", "clock")}${metric("已确认金额", money(rows.filter(order => order.performanceConfirmed).reduce((sum, order) => sum + order.paid, 0)), "", "实时", "green", "check")}</section>
    <section class="data-panel"><div class="data-toolbar"><div class="panel-title"><h2>业绩列表</h2><span>仅已回款订单可确认业绩，未回款订单需先登记回款</span></div><span class="pill gray">${pending.length} 个待确认</span></div><div class="table-wrap">${rows.length ? `<table class="data-table"><thead><tr><th>订单号</th><th>客户</th><th>销售</th><th>订单金额</th><th>已回款</th><th>支付状态</th><th>业绩状态</th><th>操作</th></tr></thead><tbody>${rows.map(order => `<tr><td>${escapeHtml(order.id)}</td><td>${escapeHtml(order.customer)}</td><td>${escapeHtml(order.owner)}</td><td>${money(order.amount)}</td><td>${money(order.paid)}</td><td>${orderPill(order.status)}</td><td><span class="pill ${order.performanceConfirmed ? "green" : order.paid > 0 ? "amber" : "gray"}">${order.performanceConfirmed ? "已确认" : order.paid > 0 ? "待确认" : "待回款"}</span></td><td>${order.performanceConfirmed ? "-" : order.paid > 0 ? `<button class="button primary" type="button" data-confirm-performance="${escapeHtml(order.id)}">确认业绩</button>` : `<span class="muted">先登记回款</span>`}</td></tr>`).join("")}</tbody></table>` : `<div class="empty-state"><span class="empty-icon">${icon("check")}</span><strong>暂无订单</strong><p>创建订单后可在这里确认业绩。</p></div>`}</div></section>
  </div></section>`; */
}

function couponManagementView() {
  return `<section class="page order-coupon-reference">${orderNav("卡券管理")}<div class="order-coupon-content"><section class="coupon-filter-panel"><div class="coupon-filter-grid"><label>卡券类型：<select><option>请选择</option></select></label><label>卡券状态：<select><option>请选择</option></select></label><label>指定门店：<select><option>请选择</option></select></label><label>核销门店：<select><option>请选择</option></select></label><label>发券时间：<input placeholder="开始日期　—　结束日期"></label><label>过期时间：<input placeholder="开始日期　—　结束日期"></label><label>使用时间：<input placeholder="开始日期　—　结束日期"></label><label>订单ID：<input placeholder="请输入订单ID"></label><label>会员ID：<input placeholder="请输入会员ID"></label></div><div class="coupon-filter-actions"><button class="button primary" type="button">${icon("search")}查询</button><button class="button secondary" type="button">${icon("repeat")}重置</button></div></section><section class="data-panel order-coupon-panel"><div class="coupon-toolbar"><div><button class="button primary" type="button">${icon("plus")}新增</button><button class="button secondary" type="button" disabled>${icon("close")}作废</button><button class="button secondary" type="button" disabled>${icon("download")}导出</button></div></div><div class="coupon-summary">共1张抵用券，券总额1元，已使用1张，优惠总额1元；1张折扣券，已使用1张，优惠总额1元；1张赠送服务券，总天数1天，已使用1张，赠送天数1天；1张料券，已使用1张，优惠总额1元</div><div class="table-wrap"><table class="data-table order-coupon-table"><thead><tr><th><input type="checkbox" aria-label="全选"></th><th>卡券ID</th><th>卡券类型</th><th>券值</th><th>券码</th><th>指定门店</th><th>发券时间</th><th>过期时间</th><th>卡券状态</th><th>使用时间</th><th>核销门店</th><th>订单ID</th></tr></thead><tbody><tr><td colspan="12"><div class="empty-state"><span class="empty-icon">${icon("wallet")}</span><strong>暂无数据</strong></div></td></tr></tbody></table></div></section></div></section>`;
  /* Existing CRM coupon view retained below for reference. */
  /* return `<section class="page">${orderNav("卡券管理")}<div class="page-content">
    ${pageHeading(`<button class="button secondary" type="button" id="exportCoupons">${icon("download")}导出卡券</button><button class="button primary" type="button" id="newCoupon">${icon("plus")}新建卡券</button>`)}
    <section class="metric-grid">${metric("卡券总数", "0", "张", "实时", "blue", "order")}${metric("可用卡券", "0", "张", "实时", "green", "check")}${metric("已核销", "0", "张", "实时", "purple", "check")}</section>
    <section class="data-panel"><div class="data-toolbar"><div class="panel-title"><h2>卡券管理</h2><span>管理订单关联卡券及核销状态</span></div><span class="pill gray">0 张</span></div><div class="empty-state"><span class="empty-icon">${icon("order")}</span><strong>暂无卡券</strong><p>新建卡券后，可在订单服务过程中发放和核销。</p><button class="button primary" type="button" id="newCouponEmpty">${icon("plus")}新建卡券</button></div></section>
  </div></section>`; */
}

function ordersView() {
  if (state.orderSection === "合同列表") return contractListView();
  if (state.orderSection === "流水列表") return paymentRecordsView();
  if (state.orderSection === "业绩上传") return performanceUploadView();
  if (state.orderSection === "退费列表") return refundView();
  if (state.orderSection === "业绩列表") return performanceView();
  if (state.orderSection === "卡券管理") return couponManagementView();
  return orderListView();
}

function analyticsSummary() {
  const activeCustomers = customers.filter(customer => customer.stage !== "已流失");
  const pipeline = activeCustomers.reduce((sum, customer) => sum + customer.amount, 0);
  const wonOrders = orders.filter(order => order.status !== "已取消");
  const wonAmount = wonOrders.reduce((sum, order) => sum + order.amount, 0);
  const paidAmount = wonOrders.reduce((sum, order) => sum + order.paid, 0);
  return {
    target: wonAmount ? (paidAmount / wonAmount * 100).toFixed(1) : "0.0",
    opportunities: activeCustomers.length,
    pipeline: (pipeline / 10000).toFixed(1),
    won: (wonAmount / 10000).toFixed(1),
    average: wonOrders.length ? (wonAmount / wonOrders.length / 10000).toFixed(2) : "0.00",
    cycle: "18"
  };
}

function analyticsNav(active) {
  const sections = ["邀约记录", "通话沟通记录", "分账记录", "库存记录", "消息统计", "通话统计", "约会列表"];
  return subnav(sections, active, sections);
}

function analyticsFilters(label) {
  return `<div class="segmented" data-range><button type="button" class="${state.range === "本月" ? "active" : ""}">本月</button><button type="button" class="${state.range === "本季度" ? "active" : ""}">本季度</button><button type="button" class="${state.range === "本年" ? "active" : ""}">本年</button></div><button class="button secondary" id="exportAnalytics" type="button">${icon("download")}${label}</button>`;
}

function customerAnalyticsView() {
  const active = customers.filter(customer => customer.stage !== "已流失");
  const highValue = active.filter(customer => customer.level === "重点客户");
  const pipeline = active.reduce((sum, customer) => sum + customer.amount, 0);
  const stages = [...new Set(customers.map(customer => customer.stage).filter(Boolean))].map(stage => {
    const rows = customers.filter(customer => customer.stage === stage);
    return [stage, rows.length, rows.reduce((sum, customer) => sum + customer.amount, 0)];
  }).sort((a, b) => b[1] - a[1]);
  const sources = [...new Set(customers.map(customer => customer.source).filter(Boolean))].map(source => {
    const rows = customers.filter(customer => customer.source === source);
    return [source, rows.length, rows.reduce((sum, customer) => sum + customer.amount, 0)];
  }).sort((a, b) => b[1] - a[1]);
  return `<section class="page">${analyticsNav("客户分析")}<div class="page-content">${pageHeading(analyticsFilters("导出客户分析"))}<section class="metric-grid">${metric("客户总数", String(customers.length), "个", "实时", "blue", "users")}${metric("活跃客户", String(active.length), "个", "实时", "green", "target")}${metric("重点客户", String(highValue.length), "个", "实时", "amber", "target")}${metric("客户池金额", money(pipeline), "", "实时", "purple", "wallet")}</section><div class="analytics-grid"><article class="panel"><header class="panel-header"><div class="panel-title"><h2>客户阶段分布</h2><span>按阶段统计客户数量和预计金额</span></div></header><div class="panel-body table-wrap"><table class="data-table"><thead><tr><th>阶段</th><th>客户数</th><th>预计金额</th><th>占比</th></tr></thead><tbody>${stages.map(row => `<tr><td>${escapeHtml(row[0])}</td><td>${row[1]}</td><td>${money(row[2])}</td><td>${customers.length ? `${Math.round(row[1] / customers.length * 100)}%` : "0%"}</td></tr>`).join("")}</tbody></table></div></article><article class="panel"><header class="panel-header"><div class="panel-title"><h2>客户来源</h2><span>识别高效获客渠道</span></div></header><div class="panel-body table-wrap"><table class="data-table"><thead><tr><th>来源</th><th>客户数</th><th>预计金额</th></tr></thead><tbody>${sources.map(row => `<tr><td>${escapeHtml(row[0])}</td><td>${row[1]}</td><td>${money(row[2])}</td></tr>`).join("")}</tbody></table></div></article></div></div></section>`;
}

function salesAnalyticsView() {
  const activeOrders = orders.filter(order => order.status !== "已取消");
  const totalAmount = activeOrders.reduce((sum, order) => sum + order.amount, 0);
  const paidAmount = activeOrders.reduce((sum, order) => sum + order.paid, 0);
  const owners = [...new Set(activeOrders.map(order => order.owner).filter(Boolean))].map(owner => {
    const rows = activeOrders.filter(order => order.owner === owner);
    const amount = rows.reduce((sum, order) => sum + order.amount, 0);
    const paid = rows.reduce((sum, order) => sum + order.paid, 0);
    return [owner, rows.length, amount, paid, amount ? Math.round(paid / amount * 100) : 0, rows.filter(order => order.performanceConfirmed).length];
  }).sort((a, b) => b[2] - a[2]);
  const paidOrders = activeOrders.filter(order => order.paid > 0).length;
  return `<section class="page">${analyticsNav("销售分析")}<div class="page-content">${pageHeading(analyticsFilters("导出销售分析"))}<section class="metric-grid">${metric("有效订单", String(activeOrders.length), "单", "实时", "blue", "order")}${metric("订单金额", money(totalAmount), "", "实时", "purple", "wallet")}${metric("已回款", money(paidAmount), "", "实时", "green", "check")}${metric("回款订单率", activeOrders.length ? `${Math.round(paidOrders / activeOrders.length * 100)}` : "0", "%", "实时", "amber", "target")}</section><section class="data-panel"><div class="data-toolbar"><div class="panel-title"><h2>销售人员业绩</h2><span>按负责人汇总订单、回款和业绩确认</span></div><span class="pill gray">${owners.length} 位销售</span></div><div class="table-wrap"><table class="data-table"><thead><tr><th>负责人</th><th>订单数</th><th>订单金额</th><th>已回款</th><th>回款率</th><th>业绩确认</th></tr></thead><tbody>${owners.map(row => `<tr><td><strong>${escapeHtml(row[0])}</strong></td><td>${row[1]}</td><td>${money(row[2])}</td><td>${money(row[3])}</td><td><span class="pill ${row[4] >= 80 ? "green" : row[4] > 0 ? "amber" : "gray"}">${row[4]}%</span></td><td>${row[5]}/${row[1]}</td></tr>`).join("")}</tbody></table></div></section></div></section>`;
}

function teamAnalyticsView() {
  const names = [...new Set([...customers.map(item => item.owner), ...orders.map(item => item.owner), ...tasks.map(item => item.owner)].filter(Boolean))];
  const rows = names.map(owner => {
    const ownerCustomers = customers.filter(item => item.owner === owner);
    const ownerOrders = orders.filter(item => item.owner === owner && item.status !== "已取消");
    const ownerTasks = tasks.filter(item => item.owner === owner);
    const amount = ownerOrders.reduce((sum, item) => sum + item.amount, 0);
    const paid = ownerOrders.reduce((sum, item) => sum + item.paid, 0);
    return [owner, ownerCustomers.length, ownerOrders.length, amount, paid, ownerTasks.length ? Math.round(ownerTasks.filter(item => item.done).length / ownerTasks.length * 100) : 0];
  }).sort((a, b) => b[3] - a[3]);
  const completedTasks = tasks.filter(task => task.done).length;
  const totalAmount = orders.filter(order => order.status !== "已取消").reduce((sum, order) => sum + order.amount, 0);
  return `<section class="page">${analyticsNav("团队分析")}<div class="page-content">${pageHeading(analyticsFilters("导出团队分析"))}<section class="metric-grid">${metric("团队成员", String(names.length), "人", "实时", "blue", "users")}${metric("团队客户", String(customers.length), "个", "实时", "green", "target")}${metric("任务完成率", tasks.length ? `${Math.round(completedTasks / tasks.length * 100)}` : "0", "%", "实时", "amber", "check")}${metric("团队订单额", money(totalAmount), "", "实时", "purple", "chart")}</section><section class="data-panel"><div class="data-toolbar"><div class="panel-title"><h2>团队成员表现</h2><span>综合客户数、订单额和任务完成率</span></div><span class="pill gray">实时数据</span></div><div class="table-wrap"><table class="data-table"><thead><tr><th>成员</th><th>客户数</th><th>有效订单</th><th>订单金额</th><th>已回款</th><th>任务完成率</th></tr></thead><tbody>${rows.map(row => `<tr><td><strong>${escapeHtml(row[0])}</strong></td><td>${row[1]}</td><td>${row[2]}</td><td>${money(row[3])}</td><td>${money(row[4])}</td><td><span class="pill ${row[5] >= 80 ? "green" : row[5] >= 50 ? "amber" : "gray"}">${row[5]}%</span></td></tr>`).join("")}</tbody></table></div></section></div></section>`;
}

function invitationDate(value) {
  return value ? formatDateTime(value).replace("T", " ") : "—";
}

function filteredInvitations() {
  const f = state.invitationFilters;
  const includes = (value, query) => !query || String(value || "").toLowerCase().includes(query.toLowerCase());
  const inRange = (value, from, to) => (!from || value >= from) && (!to || value.slice(0, 10) <= to);
  return invitations.filter(row => includes(row.inviter, f.inviter) && includes(row.customerName, f.customer) && includes(row.customerId, f.customerId)
    && (!f.method || row.invitationMethod === f.method) && (!f.store || row.storeName === f.store) && (!f.arrivalStatus || row.arrivalStatus === f.arrivalStatus)
    && inRange(row.createdAt, f.createdFrom, f.createdTo) && inRange(row.scheduledAt, f.scheduledFrom, f.scheduledTo) && (!row.arrivalAt || inRange(row.arrivalAt, f.arrivalFrom, f.arrivalTo))
    && includes(row.arrivalStatus, f.arrivalText) && includes(row.relatedOrderNo, f.orderNo) && (!f.gender || row.gender === f.gender)
    && (!f.onlyFirst || row.arrivalStatus === "会员已到本店1次"));
}

function invitationModal() {
  if (!state.invitationModalOpen) return "";
  const available = customers.filter(customer => customer.owner !== "公海");
  return `<div class="modal-backdrop invitation-modal-backdrop"><section class="modal invitation-entry-modal" role="dialog" aria-modal="true" aria-labelledby="invitationModalTitle"><header class="modal-header"><div><h2 id="invitationModalTitle">新增邀约记录</h2><p>记录客户的邀约方式和计划到店时间</p></div><button class="icon-button" type="button" data-close-invitation aria-label="关闭">${icon("close")}</button></header><form id="invitationForm" class="modal-body invitation-form"><label>客户<span>*</span><select name="customerId" required><option value="">请选择客户</option>${available.map(c => `<option value="${escapeHtml(c.id)}">${escapeHtml(c.name)} · ${escapeHtml(c.id)}</option>`).join("")}</select></label><label>邀约方式<span>*</span><select name="invitationMethod" required><option>电话邀约</option><option>微信邀约</option><option>自然到店</option><option>转介绍</option></select></label><label>见面门店<span>*</span><select name="storeName" required><option>优爱天津店</option><option>优爱北京店</option><option>优爱上海店</option></select></label><label>预约时间<span>*</span><input name="scheduledAt" type="datetime-local" required></label><label class="wide">备注<textarea name="remark" rows="3" placeholder="填写邀约需求或注意事项"></textarea></label><footer class="modal-footer wide"><button class="button secondary" type="button" data-close-invitation>取消</button><button class="button primary" type="submit">保存邀约</button></footer></form></section></div>`;
}

function invitationRecordsView() {
  const f = state.invitationFilters;
  const rows = filteredInvitations();
  const totalPages = Math.max(1, Math.ceil(rows.length / state.invitationPageSize));
  state.invitationPage = Math.min(state.invitationPage, totalPages);
  const start = (state.invitationPage - 1) * state.invitationPageSize;
  const pageRows = rows.slice(start, start + state.invitationPageSize);
  const dateField = (label, prefix) => `<label><strong>${label}：</strong><span class="invite-date-pair"><input type="date" data-invite-filter="${prefix}From" value="${f[`${prefix}From`]}"><b>—</b><input type="date" data-invite-filter="${prefix}To" value="${f[`${prefix}To`]}"></span></label>`;
  const select = (label, key, placeholder, options) => `<label><strong>${label}：</strong><select data-invite-filter="${key}"><option value="">${placeholder}</option>${options.map(v => `<option ${f[key] === v ? "selected" : ""}>${v}</option>`).join("")}</select></label>`;
  const tableRows = pageRows.map(row => `<tr><td><input type="checkbox" aria-label="选择 ${escapeHtml(row.customerName)}"></td><td>${escapeHtml(row.department)}</td><td>${escapeHtml(row.inviter)}</td><td>${escapeHtml(row.customerName)}</td><td><button class="link-button" data-open-customer="${escapeHtml(row.customerId)}">${escapeHtml(row.customerId)}</button></td><td>${escapeHtml(row.gender)}</td><td>${escapeHtml(row.birthYear)}</td><td>${escapeHtml(row.maritalStatus)}</td><td>${escapeHtml(row.annualIncome)}</td><td>${invitationDate(row.createdAt)}</td><td>${invitationDate(row.scheduledAt)}</td><td><span class="invite-status ${row.arrivalStatus === "待到店" ? "waiting" : row.arrivalStatus === "已取消" ? "cancelled" : "arrived"}">${escapeHtml(row.arrivalStatus)}</span></td><td>${escapeHtml(row.storeName)}</td><td>${row.arrivalAt ? invitationDate(row.arrivalAt) : "—"}</td><td>${escapeHtml(row.source)}</td><td>${escapeHtml(row.referrer || "—")}</td><td>${escapeHtml(row.relatedOrderNo || "—")}</td><td title="${escapeHtml(row.remark || "")}">${escapeHtml(row.remark || "—")}</td><td class="operation-column">${row.arrivalStatus === "待到店" ? `<button class="text-button" data-mark-arrival="${escapeHtml(row.id)}">到店登记</button>` : `<button class="text-button" data-open-customer="${escapeHtml(row.customerId)}">查看客户</button>`}</td></tr>`).join("");
  const pages = Array.from({length: Math.min(totalPages, 5)}, (_, i) => i + 1).map(page => `<button class="page-button ${page === state.invitationPage ? "active" : ""}" data-invitation-page="${page}">${page}</button>`).join("");
  return `<section class="page invitation-record-page">${analyticsNav("邀约记录")}<div class="page-content"><section class="invitation-filter"><div class="invitation-filter-grid"><label><strong>邀约人：</strong><input data-invite-filter="inviter" value="${escapeHtml(f.inviter)}" placeholder="请输入邀约人"></label><label><strong>用户姓名：</strong><input data-invite-filter="customer" value="${escapeHtml(f.customer)}" placeholder="请输入用户姓名"></label><label><strong>用户ID：</strong><input data-invite-filter="customerId" value="${escapeHtml(f.customerId)}" placeholder="请输入用户ID"></label>${select("见面方式", "method", "请选择", ["电话邀约","微信邀约","自然到店","转介绍"])}${select("见面门店", "store", "请选择", ["优爱天津店","优爱北京店","优爱上海店"])}${select("是否见到", "arrivalStatus", "请选择", ["待到店","会员已到本店1次","已取消"])}${dateField("创建时间", "created")}${dateField("预约时间", "scheduled")}${dateField("见面时间", "arrival")}<label><strong>客户类型：</strong><select data-invite-filter="customerType"><option value="">请选择</option><option>会员</option><option>潜在客户</option></select></label><label><strong>到店情况：</strong><input data-invite-filter="arrivalText" value="${escapeHtml(f.arrivalText)}" placeholder="请输入到店情况"></label><label><strong>关联订单：</strong><input data-invite-filter="orderNo" value="${escapeHtml(f.orderNo)}" placeholder="请输入关联订单"></label><label class="invite-radio"><strong>性别：</strong><span><input type="radio" name="inviteGender" value="" ${!f.gender ? "checked" : ""}>不限　<input type="radio" name="inviteGender" value="男" ${f.gender === "男" ? "checked" : ""}>男　<input type="radio" name="inviteGender" value="女" ${f.gender === "女" ? "checked" : ""}>女</span></label><label class="invite-check"><strong>只看首次：</strong><input id="inviteOnlyFirst" type="checkbox" ${f.onlyFirst ? "checked" : ""}></label></div><div class="invitation-filter-actions"><button class="button primary" id="queryInvitations">${icon("search")}查询</button><button class="button secondary" id="resetInvitations">${icon("repeat")}重置</button></div></section><section class="invitation-table-panel"><div class="invitation-toolbar"><button class="button primary" id="newInvitation">${icon("plus")}到店登记</button><span>共 <strong>${rows.length}</strong> 条邀约记录</span></div><div class="table-wrap"><table class="data-table invitation-table"><thead><tr><th><input type="checkbox" aria-label="全选"></th><th>所属部门</th><th>邀约人</th><th>客户姓名</th><th>ID</th><th>性别</th><th>出生年份</th><th>婚况</th><th>收入</th><th>创建时间</th><th>预约时间</th><th>到店情况</th><th>见面门店</th><th>到店时间</th><th>来源</th><th>转介绍人</th><th>关联订单</th><th>备注</th><th class="operation-column">操作</th></tr></thead><tbody>${tableRows || `<tr><td colspan="19"><div class="empty-state"><strong>没有符合条件的邀约记录</strong></div></td></tr>`}</tbody></table></div><div class="pagination"><span>${rows.length ? `${start + 1}-${Math.min(start + state.invitationPageSize, rows.length)}` : "0"} 共 ${rows.length} 条</span><div>${pages}</div><select id="invitationPageSize"><option ${state.invitationPageSize === 10 ? "selected" : ""}>10</option><option ${state.invitationPageSize === 20 ? "selected" : ""}>20</option><option ${state.invitationPageSize === 50 ? "selected" : ""}>50</option></select><span>条/页</span></div></section></div>${invitationModal()}</section>`;
}

function ledgerModal() {
  if (!state.ledgerModalOpen) return "";
  const used = new Set(ledgerAccounts.map(row => row.orderNo));
  const available = orders.filter(order => order.paid > 0 && !used.has(order.id));
  return `<div class="modal-backdrop ledger-modal-backdrop"><section class="modal ledger-entry-modal" role="dialog" aria-modal="true" aria-labelledby="ledgerModalTitle"><header class="modal-header"><div><h2 id="ledgerModalTitle">新增分账记录</h2><p>从已有回款订单创建分账</p></div><button class="icon-button" type="button" data-close-ledger aria-label="关闭">${icon("close")}</button></header><form id="ledgerForm" class="modal-body ledger-form"><label>关联订单<span>*</span><select name="orderNo" id="ledgerOrderNo" required><option value="">请选择已回款订单</option>${available.map(o => `<option value="${escapeHtml(o.id)}" data-paid="${o.paid}" data-owner="${escapeHtml(o.owner)}">${escapeHtml(o.id)} · ${escapeHtml(o.customer)} · ${money(o.paid)}</option>`).join("")}</select></label><label>所属门店<span>*</span><select name="storeName" required><option>优爱天津店</option><option>优爱北京店</option><option>优爱上海店</option></select></label><label>分账收款商编<span>*</span><input name="beneficiary" id="ledgerBeneficiary" required placeholder="例如：李凤珠 · 渠道账户"></label><label>分账金额<span>*</span><input name="ledgerAmount" id="ledgerAmount" type="number" min="0.01" step="0.01" required placeholder="0.00"></label>${available.length ? "" : `<p class="wide ledger-form-hint">当前没有可创建分账的已回款订单。</p>`}<footer class="modal-footer wide"><button class="button secondary" type="button" data-close-ledger>取消</button><button class="button primary" type="submit" ${available.length ? "" : "disabled"}>创建分账</button></footer></form></section></div>`;
}

function ledgerRecordsView() {
  const f = state.ledgerFilters;
  const rows = ledgerAccounts.filter(row => (!f.status || row.executionStatus === f.status) && (!f.store || row.storeName === f.store) && (!f.from || (row.allocatedAt || "") >= f.from) && (!f.to || (row.allocatedAt || "").slice(0,10) <= f.to));
  const body = rows.map(row => `<tr><td><button class="link-button" title="${escapeHtml(row.id)}">${escapeHtml(row.id)}</button></td><td>${escapeHtml(row.storeName)}</td><td>${escapeHtml(row.sourceMerchant)}</td><td>${escapeHtml(row.flowNo)}</td><td>${money(row.orderAmount)}</td><td>${money(row.feeAmount)}</td><td><strong>${money(row.ledgerAmount)}</strong></td><td>${escapeHtml(row.beneficiary)}</td><td><span class="ledger-status ${row.executionStatus === "执行成功" ? "success" : row.executionStatus === "待执行" ? "pending" : "inactive"}">${escapeHtml(row.executionStatus)}</span>${row.executionStatus === "待执行" ? `<button class="text-button ledger-execute" data-execute-ledger="${escapeHtml(row.id)}">执行</button>` : ""}</td><td>${invitationDate(row.allocatedAt)}</td><td>${invitationDate(row.paidOutAt)}</td><td>${escapeHtml(row.transactionOrderNo || "—")}</td></tr>`).join("");
  return `<section class="page ledger-record-page">${analyticsNav("分账记录")}<div class="page-content"><section class="ledger-filter"><div class="ledger-filter-row"><label><strong>执行状态：</strong><select id="ledgerStatus"><option value="">请选择</option>${["待执行","执行成功","未满足条件"].map(v=>`<option ${f.status===v?"selected":""}>${v}</option>`).join("")}</select></label><label><strong>所属门店：</strong><select id="ledgerStore"><option value="">请选择门店</option>${["优爱天津店","优爱北京店","优爱上海店"].map(v=>`<option ${f.store===v?"selected":""}>${v}</option>`).join("")}</select></label><label class="ledger-date"><strong>分账时间：</strong><span><input id="ledgerFrom" type="date" value="${f.from}"><b>—</b><input id="ledgerTo" type="date" value="${f.to}"></span></label></div><div class="ledger-filter-actions"><button class="button primary" id="queryLedgers">${icon("search")}查询</button><button class="button secondary" id="resetLedgers">${icon("repeat")}重置</button></div></section><section class="ledger-table-panel"><div class="ledger-toolbar"><button class="button primary" id="newLedger">${icon("plus")}新增分账</button><span>共 ${rows.length} 条记录，分账金额 ${money(rows.reduce((sum,row)=>sum+row.ledgerAmount,0))}</span></div><div class="table-wrap"><table class="data-table ledger-table"><thead><tr><th>ID</th><th>门店名称</th><th>分账出款商编</th><th>流水号</th><th>订单金额</th><th>手续费</th><th>分账金额</th><th>分账收款商编</th><th>执行状态</th><th>分账时间</th><th>出款时间</th><th>交易上送订单号</th></tr></thead><tbody>${body || `<tr><td colspan="12"><div class="ledger-empty"><span class="empty-icon">${icon("wallet")}</span><strong>暂无数据</strong></div></td></tr>`}</tbody></table></div></section></div>${ledgerModal()}</section>`;
}

function analyticsView() {
  if (state.analyticsSection === "邀约记录") return invitationRecordsView();
  if (state.analyticsSection === "分账记录") return ledgerRecordsView();
  if (state.analyticsSection === "客户分析") return customerAnalyticsView();
  if (state.analyticsSection === "销售分析") return salesAnalyticsView();
  if (state.analyticsSection === "团队分析") return teamAnalyticsView();
  const summary = analyticsSummary();
  return `<section class="page">${analyticsNav(state.analyticsSection)}<div class="page-content">
    ${pageHeading(analyticsFilters("导出经营报表"))}
    <section class="metric-grid">${metric("销售目标", summary.target, "%", "实时", "blue", "target")}${metric("新增商机", String(summary.opportunities), "个", "实时", "green", "users")}${metric("商机金额", summary.pipeline, "万元", "实时", "amber", "wallet")}${metric("赢单金额", summary.won, "万元", "实时", "purple", "chart")}${metric("客单价", summary.average, "万元", "实时", "blue", "order")}${metric("平均周期", summary.cycle, "天", "", "red", "clock", true)}</section>
    <div class="analytics-grid">
      <article class="panel"><header class="panel-header"><div class="panel-title"><h2>销售额与回款趋势</h2><span>单位：万元</span></div><div class="legend"><span>销售额</span><span>回款额</span></div></header><div class="panel-body"><div class="chart-wrap"><svg viewBox="0 0 760 250" preserveAspectRatio="none">${[28,76,124,172,220].map(y => `<line class="grid-line" x1="40" y1="${y}" x2="740" y2="${y}"/>`).join("")}<path d="M40 194 C120 180 130 145 210 151 S302 185 380 113 S475 137 548 76 S661 88 740 38" fill="none" stroke="#1677ff" stroke-width="3"/><path d="M40 213 C122 203 146 175 210 179 S310 193 380 149 S482 161 548 121 S663 130 740 88" fill="none" stroke="#0b9b84" stroke-width="3"/></svg></div></div></article>
      <article class="panel"><header class="panel-header"><div class="panel-title"><h2>客户来源分布</h2><span>共 320 条</span></div></header><div class="panel-body"><div class="donut-wrap"><div class="donut"><div class="donut-center"><strong>320</strong><span>新增客户</span></div></div><div class="donut-legend"><span><i></i>线上咨询 37%</span><span><i></i>老客转介绍 26%</span><span><i></i>市场活动 19%</span><span><i></i>主动开发 18%</span></div></div></div></article>
      <article class="panel"><header class="panel-header"><div class="panel-title"><h2>各部门业绩</h2><span>目标完成率</span></div></header><div class="panel-body"><div class="bar-chart">${[[72,"销售一部"],[58,"销售二部"],[84,"大客户部"],[46,"渠道部"],[66,"电销部"]].map(item => `<div class="bar-item"><span style="height:${item[0]}%" title="${item[0]}%"></span><small>${item[1]}</small></div>`).join("")}</div></div></article>
      <article class="panel"><header class="panel-header"><div class="panel-title"><h2>核心指标</h2><span>环比变化</span></div></header><div class="panel-body"><div class="funnel-list"><div class="funnel-item"><span class="funnel-label">线索转客户</span><div class="funnel-track"><div class="funnel-fill" style="width:72%"></div></div><strong>72%</strong></div><div class="funnel-item"><span class="funnel-label">客户转商机</span><div class="funnel-track"><div class="funnel-fill" style="width:54%"></div></div><strong>54%</strong></div><div class="funnel-item"><span class="funnel-label">商机赢单率</span><div class="funnel-track"><div class="funnel-fill" style="width:31%"></div></div><strong>31%</strong></div><div class="funnel-item"><span class="funnel-label">复购率</span><div class="funnel-track"><div class="funnel-fill" style="width:42%"></div></div><strong>42%</strong></div></div></div></article>
    </div>
  </div></section>`;
}

const systemUserRecords = [
  { id: 24863, account: "zt2", name: "张鹏", gender: "男", phone: "15022589149", storeDept: "优爱天津店 · 销售部", department: "—" },
  { id: 19960, account: "zq", name: "张倩", gender: "女", phone: "15022110855", storeDept: "优爱天津店 · 销售部", department: "—" },
  { id: 19946, account: "lt", name: "刘婷", gender: "男", phone: "13820501822", storeDept: "优爱天津店 · 销售部", department: "—" },
  { id: 14925, account: "yyn", name: "于亚楠", gender: "女", phone: "18522792875", storeDept: "优爱 · 优爱天津店", department: "—" },
  { id: 14537, account: "zhit", name: "智钢", gender: "男", phone: "15620361751", storeDept: "优爱天津店 · 销售部", department: "—" },
  { id: 14528, account: "xbx", name: "夏冰鑫", gender: "女", phone: "15022084929", storeDept: "优爱天津店 · 销售部", department: "—" },
  { id: 14345, account: "sxs", name: "孙雪嘉", gender: "男", phone: "17716510736", storeDept: "优爱天津店 · 销售部", department: "—" },
  { id: 13549, account: "lifz", name: "李凤珠", gender: "女", phone: "13312070399", storeDept: "优爱天津店 · 销售部", department: "—" },
  { id: 9921, account: "zd", name: "赵娜", gender: "男", phone: "18622738261", storeDept: "优爱 · 优爱天津店", department: "优爱天津店" },
  { id: 9106, account: "article", name: "article", gender: "女", phone: "18888888889", storeDept: "优爱天津店 · 销售部", department: "—" }
];

function systemView() {
  const sections = ["用户管理", "菜单管理", "部门管理", "业务设置"];
  const active = sections.includes(state.systemSection) ? state.systemSection : sections[0];
  if (active === "用户管理") {
    const records = state.systemUsers;
    const userRows = records.map(user => `<tr><td><input type="checkbox" aria-label="选择${user.name}"></td><td>${user.id}</td><td>${user.account}</td><td>${user.name}</td><td>${user.gender}</td><td>${user.phone}</td><td>${user.storeDept}</td><td>${user.department}</td><td><span class="pill green">正常</span></td><td><button class="table-link" type="button" data-edit-system-user="${user.id}">编辑</button><button class="table-link table-more-link" type="button">更多<span class="dropdown-chevron"></span></button></td></tr>`).join("");
    return `<section class="page system-user-reference">${subnav(sections, active, sections)}<div class="system-user-content"><section class="system-user-filter"><div class="system-user-filter-row"><label>姓名：<input placeholder="输入姓名模糊查询"></label><label>门店：<select><option>请选择门店</option></select></label><label>部门：<select><option>请选择部门</option></select></label></div><div class="system-user-actions"><button class="button primary" type="button">${icon("search")}查询</button><button class="button primary" type="button">${icon("repeat")}重置</button><button class="text-button dropdown-trigger" type="button">展开<span class="dropdown-chevron"></span></button></div><button class="button primary add-user-button" type="button">${icon("plus")}添加用户</button></section><section class="data-panel system-user-panel"><div class="selected-user-bar">已选择 <strong>0</strong> 项　<a>清空</a></div><div class="table-wrap"><table class="data-table system-user-table"><thead><tr><th><input type="checkbox" aria-label="全选"></th><th>ID</th><th>账号</th><th>姓名</th><th>性别</th><th>手机号码</th><th>门店-部门</th><th>负责部门</th><th>状态</th><th>操作</th></tr></thead><tbody>${userRows}</tbody></table></div></section></div></section>`;
  }
  if (active === "菜单管理") {
    const menus = [["首页","0","home","dashboard/Analysis","/dashboard/analysis","0"],["客户管理","0","team","layouts/RouteView","/member","1"],["学习中心权限","2","","","","1"],["学习中心","0","","layouts/RouteView","/studyCenter","1.1"],["资料审核","0","audit","layouts/RouteView","/material_check","2"],["呼叫中心","0","phone","layouts/RouteView","/call","2"],["订单管理","0","dollar","layouts/RouteView","/order","3"],["消息中心","0","aliwangwang","layouts/RouteView","/msg","3"],["系统管理","0","setting","layouts/RouteView","/system","4"],["工具管理","0","tool","layouts/RouteView","/tools","8"],["个人页","0","user","layouts/RouteView","/account","9"],["数据中心","0","bar-chart","layouts/RouteView","/report","10"],["运营管理","0","radar-chart","layouts/RouteView","/cms","11"]];
    const rows = menus.map(menu => `<tr><td><input type="checkbox" aria-label="选择${menu[0]}"></td><td><span class="menu-name">${menu[0] === "学习中心权限" ? "" : "+ "}${menu[0]}</span></td>${menu.slice(1).map(value => `<td>${value}</td>`).join("")}<td><button class="table-link">编辑</button><button class="table-link table-more-link">更多<span class="dropdown-chevron"></span></button></td></tr>`).join("");
    return `<section class="page system-menu-reference">${subnav(sections, active, sections)}<div class="system-menu-content"><section class="data-panel system-menu-panel"><div class="menu-toolbar"><button class="button primary" type="button">${icon("plus")}新增</button></div><div class="selected-user-bar">已选择 <strong>0</strong> 项　<a>清空</a></div><div class="table-wrap"><table class="data-table system-menu-table"><thead><tr><th><input type="checkbox" aria-label="全选"></th><th>菜单名称</th><th>菜单类型</th><th>Icon</th><th>组件</th><th>路径</th><th>排序</th><th>操作</th></tr></thead><tbody>${rows}</tbody></table></div></section></div></section>`;
  }
  if (active === "部门管理") {
    return `<section class="page system-department-reference">${subnav(sections, active, sections)}<div class="department-layout"><section class="department-tree-panel"><div class="department-actions"><button class="button primary" type="button">添加下级</button><button class="button secondary" type="button">批量删除</button></div><div class="department-current"><span class="department-dot"></span>当前选择：</div><label class="department-search"><input placeholder="请输入部门名称"><span>${icon("search")}</span></label><div class="department-tree"><div class="tree-node root"><span>⌄</span><strong>优爱</strong></div><div class="tree-node child"><span>⌄</span><strong>优爱天津店</strong></div><div class="tree-node leaf"><span>□</span><strong>销售部</strong></div></div><button class="button secondary department-collapse" type="button">树操作件⌃</button></section><section class="department-info-panel"><div class="department-info-title">基本信息</div><div class="department-empty"><span class="empty-icon">${icon("file")}</span><strong>请先选择一个部门！</strong></div></section></div></section>`;
  }
  const content = active === "用户管理"
    ? `<section class="data-panel"><div class="data-toolbar"><div class="panel-title"><h2>用户管理</h2><span>查看系统用户及使用状态</span></div><span class="pill gray">4 个用户</span></div><div class="table-wrap"><table class="data-table"><thead><tr><th>姓名</th><th>账号</th><th>角色</th><th>部门</th><th>状态</th></tr></thead><tbody>${["林夕","陈晨","赵磊","周倩"].map((name, index) => `<tr><td><strong>${name}</strong></td><td>user${index + 1}</td><td>${index === 0 ? "管理员" : "销售顾问"}</td><td>销售部</td><td><span class="pill green">正常</span></td></tr>`).join("")}</tbody></table></div></section>`
    : active === "菜单管理"
      ? `<section class="data-panel"><div class="data-toolbar"><div class="panel-title"><h2>菜单管理</h2><span>配置系统菜单及访问权限</span></div></div><div class="empty-state"><span class="empty-icon">${icon("sliders")}</span><strong>菜单权限配置</strong><p>管理员可在此维护菜单及角色权限。</p></div></section>`
      : active === "部门管理"
        ? `<section class="data-panel"><div class="data-toolbar"><div class="panel-title"><h2>部门管理</h2><span>维护组织架构与部门信息</span></div></div><div class="empty-state"><span class="empty-icon">${icon("users")}</span><strong>部门管理</strong><p>可在此维护部门和上下级关系。</p></div></section>`
        : `<section class="data-panel"><div class="data-toolbar"><div class="panel-title"><h2>业务设置</h2><span>维护系统通用业务参数</span></div></div><div class="empty-state"><span class="empty-icon">${icon("sliders")}</span><strong>业务设置</strong><p>配置企业信息、业务规则和通知选项。</p></div></section>`;
  return `<section class="page">${subnav(sections, active, sections)}<div class="page-content">${pageHeading()}${content}</div></section>`;
}

function financeView() {
  const sections = ["财务概览", "收款流水", "退款明细"];
  const active = sections.includes(state.financeSection) ? state.financeSection : sections[0];
  const validOrders = orders.filter(order => order.status !== "已取消");
  const receivable = validOrders.reduce((sum, order) => sum + order.amount, 0);
  const received = validOrders.reduce((sum, order) => sum + order.paid, 0);
  const refunded = state.orderRefunds.reduce((sum, refund) => sum + refund.amount, 0);
  const rows = active === "退款明细" ? state.orderRefunds : validOrders.filter(order => active !== "收款流水" || order.paid > 0);
  const table = active === "退款明细"
    ? (rows.length ? `<table class="data-table"><thead><tr><th>退款编号</th><th>订单号</th><th>客户</th><th>退款金额</th><th>状态</th></tr></thead><tbody>${rows.map(refund => `<tr><td>${escapeHtml(refund.id)}</td><td>${escapeHtml(refund.orderId)}</td><td>${escapeHtml(refund.customer)}</td><td><strong>${money(refund.amount)}</strong></td><td><span class="pill ${orderStatusClass(refund.status)}">${escapeHtml(refund.status)}</span></td></tr>`).join("")}</tbody></table>` : `<div class="empty-state"><span class="empty-icon">${icon("wallet")}</span><strong>暂无退款明细</strong></div>`)
    : `<table class="data-table"><thead><tr><th>订单号</th><th>客户</th><th>应收金额</th><th>已收金额</th><th>未收金额</th><th>状态</th></tr></thead><tbody>${rows.map(order => `<tr><td>${escapeHtml(order.id)}</td><td>${escapeHtml(order.customer)}</td><td>${money(order.amount)}</td><td><strong>${money(order.paid)}</strong></td><td>${money(Math.max(0, order.amount - order.paid))}</td><td>${orderPill(order.status)}</td></tr>`).join("")}</tbody></table>`;
  return `<section class="page">${subnav(sections, active, sections)}<div class="page-content">${pageHeading()}<section class="metric-grid">${metric("应收总额", money(receivable), "", "实时", "blue", "wallet")}${metric("实收总额", money(received), "", "实时", "green", "check")}${metric("待收金额", money(receivable - received), "", "实时", "amber", "clock")}${metric("退款金额", money(refunded), "", "实时", "red", "arrow-down")}</section><section class="data-panel"><div class="data-toolbar"><div class="panel-title"><h2>${active}</h2><span>订单财务数据实时汇总</span></div></div><div class="table-wrap">${table}</div></section></div></section>`;
}

function enhanceCustomerListColumns() {
  const table = document.querySelector(".customer-list-page:not(.pool-page) .customer-detail-table");
  if (!table || table.dataset.customerColumnsReady === "true") return;
  const operationHeader = table.querySelector("thead th:last-child");
  ["客户状态", "首次分配时间", "最后跟进时间"].forEach(label => {
    const header = document.createElement("th");
    header.textContent = label;
    operationHeader.before(header);
  });
  table.querySelectorAll("tbody tr[data-customer-id]").forEach(row => {
    const customer = customers.find(item => item.id === row.dataset.customerId);
    if (!customer) return;
    const cells = row.querySelectorAll("td");
    if (cells[11]) cells[11].textContent = String(customerFollowUpCount(customer));
    if (cells[12]) cells[12].textContent = customerUncontactedDaysLabel(customer);
    const operation = row.querySelector("td:last-child");
    [customer.stage || "—", formatDateTime(customer.firstAllocationAt || customer.lastAllocationAt), formatDateTime(customer.lastContactAt || customer.updatedAt)].forEach(value => {
      const cell = document.createElement("td");
      cell.textContent = value;
      operation.before(cell);
    });
  });
}

function render() {
  if (!viewMeta[state.view]) state.view = "dashboard";
  ensureCurrentWorkspaceTab();
  const views = { dashboard: dashboardView, customers: customersView, tasks: tasksView, calls: callsView, messages: messagesView, orders: ordersView, system: systemView, analytics: analyticsView, finance: financeView };
  document.querySelector("#app").innerHTML = views[state.view]();
  if (document.querySelector(".pool-scene-bar")) renderSharedPoolNavigation();
  if (state.view === "customers") {
    renderImportedCustomerFields();
    enhanceCustomerListColumns();
  }
  document.querySelector(".pool-page .customer-scene-bar .text-button")?.addEventListener("click", openPoolTagFilter);
  document.querySelectorAll(".nav-item").forEach(item => item.classList.toggle("active", item.dataset.view === state.view));
  renderWorkspaceTabs();
  document.title = `${workspaceTabLabel(state.view, currentWorkspaceSection())} - 优爱 YOUAI`;
  bindViewEvents();
  updateNotificationChrome();
}

function renderSharedPoolNavigation() {
  const scene = document.querySelector(".pool-scene-bar");
  const filter = document.querySelector(".pool-filter-panel");
  if (scene) scene.outerHTML = `<div class="customer-scene-bar"><span>场景：</span><button class="scene-chip active" type="button">重复注册未跟进</button><span class="pool-label">标注/标签：</span><button class="tag-filter" type="button">无标签</button><button class="tag-filter" type="button">重点客户</button><button class="tag-filter" type="button">普通客户</button><button class="text-button" type="button">更多</button></div>`;
  if (filter) filter.outerHTML = `<section class="filter-panel"><div class="filter-row"><label class="field"><span>筛选条件</span><input placeholder="ID/手机号"></label><label class="field"><span>&nbsp;</span><input placeholder="姓名/昵称/备注"></label><label class="field"><span>&nbsp;</span><select><option>全部</option></select></label><label class="field"><span>&nbsp;</span><input placeholder="请选择客户状态"></label><label class="field"><span>&nbsp;</span><select><option>全部负责人</option></select></label><label class="field"><span>&nbsp;</span><input placeholder="分配开始时间 → 分配结束时间"></label></div><div class="filter-footer"><div></div><div class="filter-actions"><button class="button primary" type="button">查询</button><button class="button secondary" type="button">重置</button><button class="text-button" type="button">高级筛选</button></div></div></section>`;
}

function openPoolTagFilter() {
  document.querySelector("#poolTagFilterBackdrop")?.remove();
  const groups = [
    ["无效组", ["不可推荐", "重复资源", "接通挂", "接通率低", "同号id", "没有离婚证", "不是本人", "刷单", "空号", "不在本地", "非单身", "同事朋友", "隐身", "内部员工", "测试数据"]],
    ["精选用户组", ["才俊佳丽"]],
    ["其他", ["同行", "骂人", "残疾人", "50岁以上"]],
    ["销售", ["投放", "库存移出", "原有资源", "测试资源", "工厂", "APP预约咨询用户", "元子时代", "爱情汇资源", "空号补偿", "到店待跟进", "二邀到店", "一邀到店"]],
    ["管理组", ["未分资源", "爱盟分佣资源V", "b8邀约资源", "爱盟分佣资源", "白银资源", "洗白资源"]],
    ["线索渠道", ["直投"]]
  ];
  const backdrop = document.createElement("div");
  backdrop.id = "poolTagFilterBackdrop";
  backdrop.className = "modal-backdrop pool-tag-backdrop";
  backdrop.innerHTML = `<section class="pool-tag-modal" role="dialog" aria-modal="true" aria-label="筛选标签"><header><h2>筛选标签</h2><button type="button" data-close-pool-tags aria-label="关闭">×</button></header><div class="pool-tag-body"><div class="pool-tag-all"><h3>全部标签(<span data-tag-total>43</span>)</h3><label class="pool-tag-search"><input type="search" placeholder="请输入"><button type="button">${icon("search")}</button></label><div class="pool-tag-groups">${groups.map(([name, tags], groupIndex) => `<section class="pool-tag-group"><div class="pool-tag-group-title"><label><input type="checkbox" data-select-tag-group="${groupIndex}"> 全选</label><i></i><strong>${name}</strong></div><div class="pool-tag-options">${tags.map(tag => `<label data-tag-label="${escapeHtml(tag)}"><input type="checkbox" value="${escapeHtml(tag)}" data-pool-tag> ${escapeHtml(tag)}</label>`).join("")}</div></section>`).join("")}</div></div><aside class="pool-tag-selected"><h3>已选(<span data-selected-count>0</span>)</h3><button type="button" data-clear-pool-tags>清空</button><div data-selected-tags></div></aside></div></section>`;
  document.body.appendChild(backdrop);
  const updateSelected = () => {
    const selected = [...backdrop.querySelectorAll("[data-pool-tag]:checked")].map(input => input.value);
    backdrop.querySelector("[data-selected-count]").textContent = selected.length;
    backdrop.querySelector("[data-selected-tags]").innerHTML = selected.map(tag => `<button type="button" data-remove-pool-tag="${escapeHtml(tag)}">${escapeHtml(tag)} ×</button>`).join("");
  };
  backdrop.addEventListener("change", event => {
    if (event.target.matches("[data-select-tag-group]")) {
      const group = event.target.closest(".pool-tag-group");
      group.querySelectorAll("[data-pool-tag]").forEach(input => { input.checked = event.target.checked; });
    }
    updateSelected();
  });
  backdrop.addEventListener("click", event => {
    if (event.target === backdrop || event.target.closest("[data-close-pool-tags]")) backdrop.remove();
    if (event.target.closest("[data-clear-pool-tags]")) { backdrop.querySelectorAll("input[type=checkbox]").forEach(input => { input.checked = false; }); updateSelected(); }
    const remove = event.target.closest("[data-remove-pool-tag]");
    if (remove) { const input = [...backdrop.querySelectorAll("[data-pool-tag]")].find(item => item.value === remove.dataset.removePoolTag); if (input) input.checked = false; updateSelected(); }
  });
  backdrop.querySelector(".pool-tag-search input").addEventListener("input", event => {
    const query = event.target.value.trim();
    backdrop.querySelectorAll("[data-tag-label]").forEach(label => { label.hidden = query && !label.dataset.tagLabel.includes(query); });
  });
}

function renderImportedCustomerFields() {
  const isPool = state.customerSection === "公海列表";
  let table = document.querySelector(".customer-detail-table");
  if (!table && !isPool) {
    const wrap = document.querySelector(".customer-list-page .data-panel .table-wrap");
    wrap?.insertAdjacentHTML("afterbegin", `<table class="data-table customer-detail-table"><thead><tr></tr></thead><tbody></tbody></table>`);
    table = document.querySelector(".customer-detail-table");
  }
  if (!table) return;
  if (!isPool) {
    const columns = customerVisibleTableColumns();
    const headRow = table.querySelector("thead tr");
    const tableCustomerIds = [...table.querySelectorAll("tbody tr[data-customer-id]")].map(row => row.dataset.customerId);
    const allSelected = tableCustomerIds.length > 0 && tableCustomerIds.every(id => state.selectedCustomerIds.includes(id));
    headRow.innerHTML = [`<th class="select-column"><input id="selectPageCustomers" type="checkbox" aria-label="选择本页客户" ${allSelected ? "checked" : ""}></th>`, ...columns.map(customerTableHeaderHtml), `<th class="operation-column">操作</th>`].join("");
    table.classList.add("custom-columns-table");
    table.dataset.customerColumnsReady = "true";
    table.querySelectorAll("tbody tr[data-customer-id]").forEach(row => {
      const customer = customers.find(item => String(item.id) === row.dataset.customerId);
      if (!customer) return;
      const operation = row.querySelector("td:last-child");
      const operationHtml = operation?.querySelector(".table-actions")?.innerHTML || `<button class="table-icon" type="button" data-call="${escapeHtml(customer.id)}" aria-label="呼叫客户">${icon("phone")}</button><button class="table-icon" type="button" data-open-customer="${escapeHtml(customer.id)}" aria-label="查看详情">${icon("chevron")}</button><button class="button ghost" type="button" data-release-customer="${escapeHtml(customer.id)}">放入公海</button>`;
      row.innerHTML = `<td class="select-column"><input type="checkbox" data-select-customer="${escapeHtml(customer.id)}" aria-label="选择${escapeHtml(customer.name)}" ${state.selectedCustomerIds.includes(customer.id) ? "checked" : ""} onclick="event.stopPropagation()"></td>${columns.map(column => `<td data-customer-column="${column.key}">${customerTableColumnHtml(customer, column.key)}</td>`).join("")}<td class="operation-column"><div class="table-actions" onclick="event.stopPropagation()">${operationHtml}</div></td>`;
    });
    return;
  }
  const headers = isPool
    ? [
      { label: "", className: "select-column" }, { label: "标注", key: "tag" }, { label: "ID", key: "id" }, { label: "称呼", key: "customer" },
      { label: "性别", key: "gender" }, { label: "婚况", key: "maritalStatus" }, { label: "年龄", key: "age" }, { label: "学历", key: "education" },
      { label: "收入", key: "income" }, { label: "等级", key: "level" }, { label: "城市", key: "city" }, { label: "跟进次数", key: "followUpCount" },
      { label: "未联系天数", key: "uncontactedDays" }, { label: "前归属人", key: "owner" }, { label: "深沟时长" }, { label: "最后跟进", key: "lastFollowUpAt" },
      { label: "最后登录", key: "lastLoginAt" }, { label: "电话号码" }, { label: "生日" }, { label: "身高" }, { label: "月收入" }, { label: "年收入" },
      { label: "职业" }, { label: "住房" }, { label: "购车" }, { label: "籍贯" }, { label: "工作地" }, { label: "微信号" }, { label: "身份证号" },
      { label: "备注说明" }, { label: "来源" }, { label: "归属员工" }, { label: "操作", className: "operation-column" }
    ]
    : [
      { label: "", className: "select-column" }, { label: "标注", key: "tag" }, { label: "ID", key: "id" }, { label: "客户姓名/昵称", key: "customer" },
      { label: "性别", key: "gender" }, { label: "婚况", key: "maritalStatus" }, { label: "年龄", key: "age" }, { label: "学历", key: "education" },
      { label: "收入", key: "income" }, { label: "等级", key: "level" }, { label: "城市", key: "city" }, { label: "跟进次数", key: "followUpCount" },
      { label: "未联系天数", key: "uncontactedDays" }, { label: "归属人", key: "owner" }, { label: "邀约人", key: "inviter" }, { label: "协作人", key: "collaborator" },
      { label: "服务人", key: "servicePerson" }, { label: "操作", className: "operation-column" }
    ];
  const headRow = table.querySelector("thead tr");
  const tableCustomerIds = [...table.querySelectorAll("tbody tr[data-customer-id]")].map(row => row.dataset.customerId);
  const allSelected = tableCustomerIds.length > 0 && tableCustomerIds.every(id => state.selectedCustomerIds.includes(id));
  headRow.innerHTML = headers.map((column, index) => index === 0
    ? `<th class="select-column"><input id="selectPageCustomers" type="checkbox" aria-label="选择本页客户" ${allSelected ? "checked" : ""}></th>`
    : customerTableHeaderHtml(column)).join("");
  table.querySelectorAll("tbody tr[data-customer-id]").forEach(row => {
    const customer = customers.find(item => String(item.id) === row.dataset.customerId) || publicPoolCustomers().find(item => String(item.id) === row.dataset.customerId);
    if (!customer) return;
    const cells = row.querySelectorAll(":scope > td");
    const operation = cells[cells.length - 1];
    const operationHtml = isPool
      ? `<button class="button ghost" type="button" data-claim-customer="${escapeHtml(customer.id)}">领取</button>`
      : operation.innerHTML;
    const values = isPool
      ? [customer.gender, customer.maritalStatus, customer.age, customer.education, customer.monthlyIncome || customer.annualIncome || customer.amount, customer.level, customer.city, customer.followUpCount || 0, customer.uncontactedDays, customer.previousOwner, customer.deepTalkDuration || "00:00", customer.lastContact, customer.lastLogin, customer.phone, customer.birthday, customer.height, customer.monthlyIncome, customer.annualIncome, customer.occupation, customer.housing, customer.car, customer.nativePlace, customer.workLocation, customer.wechat, customer.idCard, customer.remark || customer.note, customer.source, customer.owner]
      : [customer.gender, customer.maritalStatus, customer.age, customer.education, customer.monthlyIncome || customer.annualIncome || customer.amount, customer.level, customer.city, customer.followUpCount || 0, customer.uncontactedDays, customer.owner, customer.inviter, customer.collaborator, customer.serviceOwner];
    const identity = `<td class="select-column"><input type="checkbox" data-select-customer="${escapeHtml(customer.id)}" aria-label="选择${escapeHtml(customer.name)}" ${state.selectedCustomerIds.includes(customer.id) ? "checked" : ""} onclick="event.stopPropagation()"></td><td>⚑</td><td><a class="table-link" onclick="event.stopPropagation()">${escapeHtml(customer.id)}</a></td><td><div class="customer-cell"><span class="person-avatar">${escapeHtml(customer.name[0])}</span><span><strong>${escapeHtml(customer.name)}</strong></span></div></td>`;
    row.innerHTML = identity + values.map(value => `<td>${escapeHtml(String(value || "—"))}</td>`).join("") + `<td class="operation-column"><div class="table-actions" onclick="event.stopPropagation()">${operationHtml}</div></td>`;
  });
}

function navigate(view) {
  if (!viewMeta[view]) return;
  closeDrawer();
  closeModal();
  closeBusinessModal();
  state.customerDetailId = null;
  state.importDetailId = null;
  state.view = view;
  location.hash = view;
  document.querySelector("#primaryNav").classList.remove("open");
  render();
  document.querySelector("#app").focus({ preventScroll: true });
  window.scrollTo({ top: 0, behavior: "smooth" });
}

function openModal(customer = null) {
  const backdrop = document.querySelector("#modalBackdrop");
  const form = document.querySelector("#customerForm");
  const field = name => form.querySelector(`[name="${name}"]`);
  form.reset();
  field("customerId").value = customer?.id || "";
  document.querySelector("#modalTitle").textContent = customer ? "编辑客户" : "创建客户";
  if (customer) {
    const profileFields = ["name", "company", "source", "level", "owner", "stage", "amount", "city", "gender", "birthday", "age", "height", "maritalStatus", "education", "monthlyIncome", "annualIncome", "occupation", "housing", "car", "vehicleHousing", "nativePlace", "workLocation", "wechat", "idCard", "certificationStatus", "familyStatus", "childrenStatus", "matchAgeRange", "matchMaritalStatus", "matchHeightRange", "matchEducation", "matchMonthlyIncome", "matchMostImportant", "matchPersonality", "matchChildren", "matchDealbreakers", "note", "remark"];
    profileFields.forEach(name => { field(name).value = customer[name] ?? ""; });
    field("phone").value = String(customer.phone || "").replace(/\D/g, "");
    field("nextFollowAt").value = toDateTimeLocal(customer.nextFollowAt);
    field("tags").value = (customer.tags || []).join(", ");
  } else {
    field("owner").value = currentOwner();
  }
  field("owner").disabled = !isAdmin();
  backdrop.classList.toggle("customer-editor-with-tabs", !document.querySelector("#workspaceTabs")?.hidden);
  backdrop.hidden = false;
  document.body.style.overflow = "hidden";
  setTimeout(() => backdrop.querySelector("input").focus(), 0);
}

function closeModal() {
  document.querySelector("#modalBackdrop").hidden = true;
  document.body.style.overflow = "";
  const form = document.querySelector("#customerForm");
  form.reset();
  form.querySelector('[name="owner"]').disabled = false;
}

let collaborationOwnerPopover = null;
let collaborationOwnerActiveRow = null;
let collaborationOwnerExpandedNodes = ["store:youai-tianjin", "group:sales"];
let collaborationOwnerQuery = "";

function collaborationOwnerTreeMatches(node, query) {
  return node.label.toLowerCase().includes(query) || (node.children || []).some(child => collaborationOwnerTreeMatches(child, query));
}

function collaborationOwnerTreeView(node, query = "") {
  const normalizedQuery = String(query || "").trim().toLowerCase();
  if (normalizedQuery && !collaborationOwnerTreeMatches(node, normalizedQuery)) return "";
  const hasChildren = Boolean(node.children?.length);
  const expanded = hasChildren && (Boolean(normalizedQuery) || collaborationOwnerExpandedNodes.includes(node.id));
  const label = node.owner
    ? `<button class="collaboration-owner-option" type="button" data-collaboration-owner-option="${escapeHtml(node.owner)}">${escapeHtml(node.label)}</button>`
    : `<span class="collaboration-owner-folder">${escapeHtml(node.label)}</span>`;
  return `<div class="collaboration-owner-tree-node"><div class="collaboration-owner-tree-row">${hasChildren ? `<button class="collaboration-owner-expand" type="button" data-collaboration-owner-expand="${escapeHtml(node.id)}" aria-expanded="${expanded ? "true" : "false"}" aria-label="${expanded ? '收起' : '展开'}${escapeHtml(node.label)}"></button>` : `<span class="collaboration-owner-indent" aria-hidden="true"></span>`}${label}</div>${expanded ? `<div class="collaboration-owner-tree-children">${(node.children || []).map(child => collaborationOwnerTreeView(child, normalizedQuery)).join("")}</div>` : ""}</div>`;
}

function closeCollaborationOwnerPopover() {
  collaborationOwnerActiveRow?.querySelector("[data-collaboration-owner-toggle]")?.setAttribute("aria-expanded", "false");
  collaborationOwnerPopover?.remove();
  collaborationOwnerPopover = null;
  collaborationOwnerActiveRow = null;
  collaborationOwnerQuery = "";
}

function renderCollaborationOwnerPopover(row, query = collaborationOwnerQuery) {
  if (!row || collaborationOwnerActiveRow !== row) return;
  collaborationOwnerQuery = query;
  const input = row.querySelector("[data-collaboration-owner-search]");
  if (!input) return;
  row.querySelector("[data-collaboration-owner-toggle]")?.setAttribute("aria-expanded", "true");
  const rect = input.getBoundingClientRect();
  if (!collaborationOwnerPopover) {
    collaborationOwnerPopover = document.createElement("div");
    collaborationOwnerPopover.className = "collaboration-owner-popover";
    collaborationOwnerPopover.addEventListener("mousedown", event => event.preventDefault());
    collaborationOwnerPopover.addEventListener("click", event => {
      const expand = event.target.closest("[data-collaboration-owner-expand]");
      if (expand) {
        const nodeId = expand.dataset.collaborationOwnerExpand;
        collaborationOwnerExpandedNodes = collaborationOwnerExpandedNodes.includes(nodeId)
          ? collaborationOwnerExpandedNodes.filter(id => id !== nodeId)
          : [...collaborationOwnerExpandedNodes, nodeId];
        renderCollaborationOwnerPopover(collaborationOwnerActiveRow);
        return;
      }
      const option = event.target.closest("[data-collaboration-owner-option]");
      if (!option || !collaborationOwnerActiveRow) return;
      const hidden = collaborationOwnerActiveRow.querySelector('input[name="collaborators"]');
      const search = collaborationOwnerActiveRow.querySelector("[data-collaboration-owner-search]");
      if (hidden) hidden.value = option.dataset.collaborationOwnerOption || "";
      if (search) search.value = option.dataset.collaborationOwnerOption || "";
      closeCollaborationOwnerPopover();
    });
    document.body.append(collaborationOwnerPopover);
  }
  collaborationOwnerPopover.style.left = `${Math.round(rect.left)}px`;
  collaborationOwnerPopover.style.top = `${Math.round(rect.bottom + 1)}px`;
  collaborationOwnerPopover.style.width = `${Math.round(rect.width)}px`;
  collaborationOwnerPopover.innerHTML = collaborationOwnerTreeView(customerOwnerTree(), query) || `<span class="collaboration-owner-empty">未找到匹配的协作人</span>`;
}

function openCollaborationOwnerPopover(row) {
  if (!row) return;
  if (collaborationOwnerActiveRow !== row) {
    closeCollaborationOwnerPopover();
    collaborationOwnerExpandedNodes = ["store:youai-tianjin", "group:sales"];
  }
  collaborationOwnerActiveRow = row;
  collaborationOwnerQuery = "";
  renderCollaborationOwnerPopover(row, "");
}

function collaborationOwnerField(selected = "") {
  return `<div class="collaboration-owner-field"><input type="hidden" name="collaborators" value="${escapeHtml(selected)}"><input class="collaboration-owner-search" type="search" data-collaboration-owner-search value="${escapeHtml(selected)}" placeholder="请选择协作人" autocomplete="off" aria-label="请选择协作人"><button class="collaboration-owner-toggle" type="button" data-collaboration-owner-toggle aria-label="展开协作人选项" aria-expanded="false"><span aria-hidden="true"></span></button></div>`;
}

function collaborationOrdinal(index) {
  return ["一", "二", "三", "四", "五", "六", "七", "八", "九", "十"][index] || String(index + 1);
}

function collaborationRow(index, selected = "") {
  return `<div class="collaboration-row" data-collaboration-row><label><span>第${collaborationOrdinal(index)}协作人</span>${collaborationOwnerField(selected)}</label><button class="collaboration-remove" type="button" data-remove-collaborator>删除</button></div>`;
}

function businessModalFields(type, record) {
  if (type === "customer-pool") {
    const selectedCustomers = Array.isArray(record?.customers)
      ? record.customers
      : [record?.customer].filter(Boolean);
    const customerIds = selectedCustomers.map(customer => customer?.id).filter(Boolean).join(",");
    return `<div class="resource-allocation-fields customer-pool-fields">
      <label class="form-span-2"><span>被移入公海ID：</span><div class="customer-pool-id-field"><textarea name="customerIds" rows="2" readonly>${escapeHtml(customerIds)}</textarea><small>已选${selectedCustomers.length}位客户</small></div></label>
      <label class="form-span-2"><span><i>*</i>类型：</span><select name="type" required><option value="归属人" selected>归属人</option></select></label>
      <div class="form-span-2 customer-pool-reason-field"><span class="customer-pool-reason-label"><i>*</i>放弃原因：</span><div class="customer-pool-reasons">${customerPoolReasons.map((reason, index) => `<label><input type="radio" name="reason" value="${escapeHtml(reason)}" ${index === 0 ? "checked" : ""} required><span>${escapeHtml(reason)}</span></label>`).join("")}</div></div>
      <label class="form-span-2 customer-pool-note-field"><span>备注信息：</span><div class="customer-pool-note-wrap"><textarea name="note" maxlength="300" rows="4" placeholder="请输入备注信息"></textarea><small>已输入 <b data-customer-pool-note-count>0</b>/300</small></div></label>
      <div class="customer-pool-rules form-span-2"><p>1：转移至公海后此客户归属于公共资源</p><p>2：客户转移入公海后，前归属人1000天内不再领取</p></div>
    </div>${resourceAllocationModalFooter()}`;
  }
  if (type === "customer-assignment") {
    const allocatedCustomers = Array.isArray(record?.customers)
      ? record.customers
      : [record?.customer].filter(Boolean);
    const customerIds = allocatedCustomers.map(customer => customer?.id).filter(Boolean).join(",");
    return `<div class="resource-allocation-fields">
      <label class="form-span-2"><span><i>*</i>被分配客户ID</span><textarea name="customerIds" class="resource-allocation-ids" rows="3" readonly>${escapeHtml(customerIds)}</textarea></label>
      <label class="form-span-2"><span><i>*</i>接受对象</span>${customerAssignmentOwnerControl(record?.owner || "")}</label>
      <label class="form-span-2"><span><i>*</i>类型</span><select name="type" required><option value="归属人" selected>归属人</option></select></label>
      <label class="form-span-2"><span><i>*</i>成熟度</span><input name="maturity" required maxlength="128" placeholder="请输入客户成熟度"></label>
      <label class="form-span-2"><span><i>*</i>分配原因</span><textarea name="reason" required maxlength="500" rows="4" placeholder="请输入分配原因"></textarea></label>
    </div>${resourceAllocationModalFooter()}`;
  }
  if (type === "collaboration") {
    const collaborators = Array.isArray(record?.collaborators)
      ? record.collaborators
      : [record?.collaborator].filter(Boolean);
    return `<input type="hidden" name="customerId" value="${escapeHtml(record?.customerId || "")}"><div class="collaboration-entry"><div class="collaboration-entry-list" data-collaboration-entries>${collaborators.map((collaborator, index) => collaborationRow(index, collaborator)).join("")}</div><button class="collaboration-add" type="button" data-add-collaborator>增加</button></div>${businessModalFooter(false, "保存")}`;
  }
  if (type === "task") {
    const dueAt = toDateTimeLocal(record?.dueAt) || toDateTimeLocal(new Date(Date.now() + 24 * 60 * 60 * 1000));
    if (record?.corner) {
      return `<input type="hidden" name="recordId" value=""><input type="hidden" name="customer" value="${escapeHtml(record.customer || "")}"><input type="hidden" name="customerId" value="${escapeHtml(record.customerId || "")}"><div class="form-grid followup-entry-fields">
        <label><span>类型 *</span><select name="type" required><option value="" selected>请选择</option>${customerFollowUpTypes.map(value => `<option value="${value}">${value}</option>`).join("")}</select></label>
        <label><span>时间 *</span><input name="dueAt" type="datetime-local" required value="${dueAt}"></label>
        <label class="form-span-2"><span>模板</span><select name="template"><option>请选择模板</option><option>首次跟进</option><option>报价跟进</option><option>会议确认</option></select></label>
        <label class="form-span-2 followup-content-field"><span>内容 *</span><textarea name="title" required maxlength="2000" rows="5" placeholder="请输入跟进内容"></textarea><small class="followup-content-count">已输入 0/2000</small></label>
        <label class="form-span-2 followup-photo-field"><span>照片</span><span class="followup-upload"><input name="photo" type="file" accept="image/*"><strong>＋</strong><small>上传</small></span></label>
        <label class="form-span-2"><span>客户状态</span><div class="followup-status-select"><input type="hidden" name="customerStatus" value=""><button type="button" class="followup-status-trigger">请选择<span class="dropdown-chevron"></span></button><div class="followup-status-options">${customerStatusOptions.map(option => `<button type="button" data-followup-status="${escapeHtml(option)}">${escapeHtml(option)}</button>`).join("")}</div></div></label>
        <label class="checkbox-field form-span-2"><input name="important" type="checkbox"><span>标为重点小计</span></label>
        <div class="followup-next-task-group form-span-2"><label class="followup-next-task"><input name="createNextTask" type="checkbox"><span class="followup-switch" aria-hidden="true"></span><span class="followup-next-copy"><strong>创建下次跟进任务</strong></span></label><div class="followup-next-fields is-collapsed"><label><span>下次跟进时间</span><input name="nextDueAt" type="datetime-local" value="${dueAt}"></label><label><span>跟进内容</span><input name="nextTitle" maxlength="2000" placeholder="请输入下次跟进内容"></label></div></div>
      </div>${businessModalFooter(false, "提交")}`;
    }
    return `<input type="hidden" name="recordId" value="${record?.id || ""}"><div class="form-grid">
      <label><span>类型 *</span><select name="type">${["电话跟进","发送资料","会议","合同","记录"].map(value => `<option ${value === record?.type ? "selected" : ""}>${value}</option>`).join("")}</select></label>
      <label><span>时间 *</span><input name="dueAt" type="datetime-local" required value="${dueAt}"></label>
      <label class="form-span-2"><span>模板</span><select><option>请选择模板</option><option>首次跟进</option><option>报价跟进</option><option>会议确认</option></select></label>
      <label class="form-span-2"><span>内容 *</span><textarea name="title" required maxlength="160" rows="5" placeholder="请输入跟进内容">${escapeHtml(record?.title || "")}</textarea></label>
      <label><span>客户 *</span><select name="customer" required>${customers.map(customer => `<option value="${escapeHtml(customer.name)}" ${customer.name === record?.customer ? "selected" : ""}>${escapeHtml(customer.name)} · ${escapeHtml(customer.company)}</option>`).join("")}</select></label>
      <label><span>客户状态</span><select><option>请选择</option>${customerStatusOptions.map(option => `<option>${option}</option>`).join("")}</select></label>
      <label><span>负责人 *</span><select name="owner" required ${isAdmin() ? "" : "disabled"}>${ownerOptions(record?.owner || currentOwner())}</select></label>
      <label><span>优先级</span><select name="priority">${["普通","高","紧急"].map(value => `<option ${value === record?.priority ? "selected" : ""}>${value}</option>`).join("")}</select></label>
      <label class="checkbox-field"><input name="completed" type="checkbox" ${record?.done ? "checked" : ""}><span>重点标记</span></label>
    </div>${businessModalFooter(Boolean(record), "提交")}`;
  }
  if (type === "order") {
    return `<input type="hidden" name="recordId" value="${record?.id || ""}"><div class="form-grid">
      <label><span>订单编号</span><input name="orderNo" maxlength="32" value="${escapeHtml(record?.id || "")}" ${record ? "readonly" : ""} placeholder="留空自动生成"></label>
      <label><span>客户 *</span><select name="customer" required>${customers.map(customer => `<option value="${escapeHtml(customer.name)}" ${customer.name === record?.customer ? "selected" : ""}>${escapeHtml(customer.name)} · ${escapeHtml(customer.company)}</option>`).join("")}</select></label>
      <label class="form-span-2"><span>商品 / 套餐 *</span><input name="product" required maxlength="128" value="${escapeHtml(record?.product || "")}" placeholder="例如：专业版 · 20 席位"></label>
      <label><span>订单金额 *</span><input name="amount" type="number" min="0" step="0.01" required value="${record?.amount ?? ""}"></label>
      <label><span>已付金额</span><input name="paid" type="number" min="0" step="0.01" value="${record?.paid ?? 0}"></label>
      <label><span>服务状态</span><select name="service">${["未开始","待开通","实施中","已开通"].map(value => `<option ${value === record?.service ? "selected" : ""}>${value}</option>`).join("")}</select></label>
      <label><span>销售负责人 *</span><select name="owner" required ${isAdmin() ? "" : "disabled"}>${ownerOptions(record?.owner || currentOwner())}</select></label>
    </div>${businessModalFooter(false, "保存订单")}`;
  }
  if (type === "order-payment") {
    return `<input type="hidden" name="recordId" value="${escapeHtml(record?.id || "")}"><div class="form-grid"><label class="form-span-2"><span>订单</span><input value="${escapeHtml(record?.id || "")} · ${escapeHtml(record?.customer || "")}" readonly></label><label class="form-span-2"><span>已付金额 *</span><input name="paid" type="number" min="0" max="${record?.amount ?? 0}" step="0.01" required value="${record?.paid ?? 0}"></label></div>${businessModalFooter(false, "保存回款")}`;
  }
  if (type === "order-service") {
    return `<input type="hidden" name="recordId" value="${escapeHtml(record?.id || "")}"><div class="form-grid"><label class="form-span-2"><span>订单</span><input value="${escapeHtml(record?.id || "")} · ${escapeHtml(record?.customer || "")}" readonly></label><label class="form-span-2"><span>服务状态 *</span><select name="service">${["未开始","待开通","实施中","已开通"].map(value => `<option ${value === record?.service ? "selected" : ""}>${value}</option>`).join("")}</select></label></div>${businessModalFooter(false, "保存服务状态")}`;
  }
  if (type === "refund") {
    const refundable = Math.max(0, Number(record?.paid || 0));
    return `<div class="form-grid"><label class="form-span-2"><span>订单 *</span><select name="orderId" required>${orders.map(order => `<option value="${escapeHtml(order.id)}" ${order.id === record?.id ? "selected" : ""}>${escapeHtml(order.id)} · ${escapeHtml(order.customer)} · 可退 ${money(order.paid)}</option>`).join("")}</select></label><label><span>退款金额 *</span><input name="refundAmount" type="number" min="0.01" max="${refundable}" step="0.01" required value="${refundable || ""}"></label><label><span>退款原因 *</span><input name="refundReason" required maxlength="160" placeholder="例如：客户取消采购"></label></div>${businessModalFooter(false, "提交退款申请")}`;
  }
  if (type === "call") {
    const matched = customers.find(customer => customer.name === record?.name);
    return `<div class="form-grid"><label><span>客户 *</span><select name="customerId" required>${customers.map(customer => `<option value="${customer.id}" ${customer.id === matched?.id ? "selected" : ""}>${escapeHtml(customer.name)} · ${escapeHtml(customer.phone)}</option>`).join("")}</select></label><label><span>呼叫结果 *</span><select name="status"><option>已接通</option><option>未接通</option><option>待回拨</option></select></label><label><span>通话时长（秒）</span><input name="durationSeconds" type="number" min="0" value="0"></label><label><span>方向</span><select name="direction"><option>呼出</option><option>呼入</option></select></label><label class="form-span-2"><span>沟通备注</span><textarea name="note" rows="4" placeholder="记录本次沟通结果和下一步安排"></textarea></label></div>${businessModalFooter(false, "保存通话记录")}`;
  }
  if (type === "conversation") {
    return `<div class="form-grid"><label class="form-span-2"><span>选择客户 *</span><select name="customerId" required>${customers.map(customer => `<option value="${customer.id}">${escapeHtml(customer.name)} · ${escapeHtml(customer.company)}</option>`).join("")}</select></label></div>${businessModalFooter(false, "创建会话")}`;
  }
  if (type === "profile") {
    const user = state.auth.user || {};
    return `<section class="detail-section"><div class="detail-grid"><div class="detail-item"><span>显示名称</span><strong>${escapeHtml(user.displayName || "-")}</strong></div><div class="detail-item"><span>登录账号</span><strong>${escapeHtml(user.username || "-")}</strong></div><div class="detail-item"><span>部门</span><strong>${escapeHtml(user.departmentName || "-")}</strong></div><div class="detail-item"><span>角色</span><strong>${escapeHtml((user.roles || []).join("、") || "-")}</strong></div></div></section>${readOnlyModalFooter()}`;
  }
  if (type === "settings") {
    return `<section class="detail-section"><div class="detail-grid"><div class="detail-item"><span>数据服务</span><strong>${state.backendOnline ? "MySQL 在线" : "服务不可用"}</strong></div><div class="detail-item"><span>接口地址</span><strong>${escapeHtml(API_BASE)}</strong></div><div class="detail-item"><span>当前权限</span><strong>${isAdmin() ? "管理员：全部数据" : "销售：本人数据"}</strong></div><div class="detail-item"><span>会话数量</span><strong>${conversations.length}</strong></div></div></section>${readOnlyModalFooter()}`;
  }
  if (type === "help") {
    return `<section class="detail-section help-content"><h3>常用操作</h3><div class="help-list"><div><strong>客户管理</strong><p>使用筛选条件查找客户，点击客户行查看详情，可直接记录通话、发送消息或编辑资料。</p></div><div><strong>跟进任务</strong><p>在任务看板中新建任务，点击任务卡编辑；左侧勾选按钮可快速标记完成。</p></div><div><strong>订单管理</strong><p>打开订单详情后，可编辑订单、登记回款、更新服务状态并确认业绩。</p></div><div><strong>消息中心</strong><p>选择客户会话后发送消息，消息会保存到 MySQL，并可使用“全部已读”清理未读提示。</p></div></div></section>${readOnlyModalFooter()}`;
  }
  if (type === "learning") {
    return `<section class="detail-section help-content"><h3>上手路径</h3><div class="help-list"><div><strong>1. 建立客户档案</strong><p>先在客户管理中录入姓名、公司、负责人和预计金额。</p></div><div><strong>2. 安排下一步</strong><p>在跟进任务中设置截止时间和优先级，避免遗漏客户触达。</p></div><div><strong>3. 留下沟通记录</strong><p>通话和消息都从客户详情进入，系统会自动汇总到客户动态。</p></div><div><strong>4. 跟踪成交结果</strong><p>在订单管理中维护回款与服务状态，数据中心可查看经营汇总。</p></div></div></section>${readOnlyModalFooter()}`;
  }
  return "";
}

function resourceAllocationModalFooter() {
  return `<footer class="modal-footer resource-allocation-footer"><button class="button primary" type="submit">${icon("check")}确定</button><button class="button secondary" data-resource-reset type="button">${icon("repeat")}重置</button></footer>`;
}

function businessModalFooter(allowDelete, submitLabel) {
  return `<footer class="modal-footer">${allowDelete ? `<button class="button danger business-delete" type="button">${icon("close")}删除</button>` : ""}<span class="modal-footer-spacer"></span><button class="button secondary" data-close-business type="button">取消</button><button class="button primary" type="submit">${icon("check")}${submitLabel}</button></footer>`;
}

function readOnlyModalFooter() {
  return `<footer class="modal-footer"><span class="modal-footer-spacer"></span><button class="button secondary" data-close-business type="button">关闭</button></footer>`;
}

function openHelpModal(kind = "help") {
  const isLearning = kind === "learning";
  const backdrop = document.querySelector("#businessModalBackdrop");
  const form = document.querySelector("#businessForm");
  document.querySelector("#businessModalEyebrow").textContent = isLearning ? "QUICK START" : "HELP CENTER";
  document.querySelector("#businessModalTitle").textContent = isLearning ? "学习中心" : "使用帮助";
  form.innerHTML = businessModalFields(isLearning ? "learning" : "help");
  form.querySelector("[data-close-business]")?.addEventListener("click", closeBusinessModal);
  backdrop.hidden = false;
  document.body.style.overflow = "hidden";
}

window.openHelpModal = openHelpModal;

function openBusinessModal(type, record = null) {
  const labels = { collaboration: ["", "增加协作"], "customer-assignment": ["", "资源调配"], "customer-pool": ["", "移入公海"], task: ["FOLLOW-UP TASK", record ? (record.corner ? "新建跟进" : "编辑任务") : "新建任务"], order: ["SALES ORDER", record ? "编辑订单" : "新建订单"], "order-payment": ["PAYMENT RECORD", "登记订单回款"], "order-service": ["SERVICE STATUS", "更新服务状态"], refund: ["REFUND REQUEST", "发起退款申请"], call: ["CALL RECORD", "记录客户通话"], conversation: ["CUSTOMER CONVERSATION", "新建会话"], profile: ["ACCOUNT PROFILE", "个人资料"], settings: ["WORKSPACE SETTINGS", "企业设置"], help: ["HELP CENTER", "使用帮助"], learning: ["QUICK START", "学习中心"] };
  const backdrop = document.querySelector("#businessModalBackdrop");
  const form = document.querySelector("#businessForm");
  backdrop.classList.toggle("followup-corner-backdrop", type === "task" && Boolean(record?.corner));
  backdrop.classList.toggle("collaboration-backdrop", type === "collaboration");
  backdrop.classList.toggle("resource-allocation-backdrop", type === "customer-assignment" || type === "customer-pool");
  backdrop.classList.toggle("customer-pool-backdrop", type === "customer-pool");
  form.dataset.type = type;
  document.querySelector("#businessModalEyebrow").textContent = labels[type][0];
  document.querySelector("#businessModalTitle").textContent = labels[type][1];
  form.innerHTML = businessModalFields(type, record);
  if (type === "collaboration") {
    const entries = form.querySelector("[data-collaboration-entries]");
    form.querySelector("[data-add-collaborator]")?.addEventListener("click", () => {
      const index = entries.children.length;
      entries.insertAdjacentHTML("beforeend", collaborationRow(index));
      entries.querySelector(`[data-collaboration-row]:last-child [data-collaboration-owner-search]`)?.focus();
    });
    entries?.addEventListener("input", event => {
      const input = event.target.closest("[data-collaboration-owner-search]");
      if (!input) return;
      const row = input.closest("[data-collaboration-row]");
      const hidden = row?.querySelector('input[name="collaborators"]');
      if (hidden) hidden.value = "";
      if (collaborationOwnerActiveRow !== row) openCollaborationOwnerPopover(row);
      renderCollaborationOwnerPopover(row, input.value);
    });
    entries?.addEventListener("click", event => {
      const toggle = event.target.closest("[data-collaboration-owner-toggle]");
      if (toggle) {
        const row = toggle.closest("[data-collaboration-row]");
        if (collaborationOwnerActiveRow === row && collaborationOwnerPopover) closeCollaborationOwnerPopover();
        else openCollaborationOwnerPopover(row);
        return;
      }
      const input = event.target.closest("[data-collaboration-owner-search]");
      if (input) {
        const row = input.closest("[data-collaboration-row]");
        if (collaborationOwnerActiveRow === row && collaborationOwnerPopover) closeCollaborationOwnerPopover();
        else openCollaborationOwnerPopover(row);
        return;
      }
      const remove = event.target.closest("[data-remove-collaborator]");
      if (!remove) return;
      const row = remove.closest("[data-collaboration-row]");
      if (collaborationOwnerActiveRow === row) closeCollaborationOwnerPopover();
      row?.remove();
      entries.querySelectorAll("[data-collaboration-row]").forEach((row, index) => {
        const label = row.querySelector("label > span");
        if (label) label.textContent = `第${collaborationOrdinal(index)}协作人`;
      });
    });
  }
  if (type === "task" && record?.corner) {
    const statusSelect = form.querySelector(".followup-status-select");
    const statusTrigger = statusSelect?.querySelector(".followup-status-trigger");
    const statusInput = statusSelect?.querySelector("[name=customerStatus]");
    statusTrigger?.addEventListener("click", () => statusSelect.classList.toggle("open"));
    statusSelect?.querySelectorAll("[data-followup-status]").forEach(option => option.addEventListener("click", () => {
      statusInput.value = option.dataset.followupStatus;
      statusTrigger.firstChild.textContent = option.textContent;
      statusSelect.classList.remove("open");
    }));
    const content = form.querySelector("textarea[name=title]");
    const contentCount = form.querySelector(".followup-content-count");
    content?.addEventListener("input", () => { contentCount.textContent = `已输入 ${content.value.length}/2000`; });
    const nextTaskToggle = form.querySelector("input[name=createNextTask]");
    const nextTaskFields = form.querySelector(".followup-next-fields");
    const nextDueAt = form.querySelector("input[name=nextDueAt]");
    const nextTitle = form.querySelector("input[name=nextTitle]");
    nextTaskToggle?.addEventListener("change", () => {
      nextTaskFields.classList.toggle("is-collapsed", !nextTaskToggle.checked);
      nextDueAt.required = nextTaskToggle.checked;
      nextTitle.required = nextTaskToggle.checked;
    });
  }
  if (type === "customer-assignment") {
    const ownerSelect = form.querySelector("[data-resource-owner-select]");
    const ownerControl = ownerSelect?.querySelector("[data-resource-owner-control]");
    const ownerMenu = ownerSelect?.querySelector("[data-resource-owner-menu]");
    ownerControl?.addEventListener("click", event => {
      const remove = event.target.closest("[data-resource-owner-remove]");
      if (remove) {
        const option = ownerSelect.querySelector(`[data-resource-owner-option][value="${CSS.escape(remove.dataset.resourceOwnerRemove)}"]`);
        if (option) option.checked = false;
        syncCustomerAssignmentOwnerControl(ownerSelect);
        event.stopPropagation();
        return;
      }
      const open = ownerMenu.hidden;
      ownerMenu.hidden = !open;
      ownerControl.setAttribute("aria-expanded", String(open));
      ownerSelect.classList.toggle("open", open);
    });
    ownerMenu?.addEventListener("change", event => {
      const option = event.target.closest("[data-resource-owner-option]");
      if (!option) return;
      if (option.checked) ownerMenu.querySelectorAll("[data-resource-owner-option]").forEach(item => { if (item !== option) item.checked = false; });
      syncCustomerAssignmentOwnerControl(ownerSelect);
    });
    ownerSelect?.addEventListener("keydown", event => {
      const remove = event.target.closest("[data-resource-owner-remove]");
      if (!remove || !["Enter", " "].includes(event.key)) return;
      event.preventDefault();
      const option = ownerSelect.querySelector(`[data-resource-owner-option][value="${CSS.escape(remove.dataset.resourceOwnerRemove)}"]`);
      if (option) option.checked = false;
      syncCustomerAssignmentOwnerControl(ownerSelect);
    });
    form.querySelector("[data-resource-reset]")?.addEventListener("click", () => form.reset());
    form.addEventListener("reset", () => setTimeout(() => syncCustomerAssignmentOwnerControl(ownerSelect), 0));
  }
  if (type === "customer-pool") {
    const note = form.querySelector("textarea[name=note]");
    const count = form.querySelector("[data-customer-pool-note-count]");
    note?.addEventListener("input", () => { if (count) count.textContent = String(note.value.length); });
    form.querySelector("[data-resource-reset]")?.addEventListener("click", () => {
      form.reset();
      if (count) count.textContent = "0";
    });
  }
  form.querySelector("[data-close-business]")?.addEventListener("click", closeBusinessModal);
  form.querySelector(".business-delete")?.addEventListener("click", () => deleteTask(record));
  backdrop.hidden = false;
  document.body.style.overflow = "hidden";
  setTimeout(() => form.querySelector("input:not([type=hidden]), select")?.focus(), 0);
}

function closeBusinessModal() {
  closeCollaborationOwnerPopover();
  document.querySelector("#businessModalBackdrop").hidden = true;
  document.querySelector("#businessForm").innerHTML = "";
  document.body.style.overflow = "";
}

function openSystemUserEditor(id) {
  const user = state.systemUsers.find(item => String(item.id) === String(id));
  if (!user) return;
  const drawer = document.querySelector("#detailDrawer");
  const backdrop = document.querySelector("#drawerBackdrop");
  drawer.innerHTML = `<header class="drawer-header"><div><h2>编辑</h2></div><button class="icon-button" data-close-drawer aria-label="关闭">${icon("close")}</button></header><div class="drawer-body system-user-editor"><div class="system-editor-id">ID：${user.id}</div><form id="systemUserEditForm" class="system-user-editor-form"><label><span><i>*</i>用户账号：</span><input name="account" value="${user.account}" required></label><label><span><i>*</i>手机号码：</span><input name="phone" value="${user.phone}" required></label><label><span><i>*</i>用户姓名：</span><input name="name" value="${user.name}" required></label><label><span>职务：</span><div class="editor-inline"><span class="editor-tag">销售 ×</span><button type="button" class="button primary">${icon("search")}选择</button></div></label><label><span><i>*</i>角色分配：</span><div class="editor-tags"><span class="editor-tag">销售 ×</span></div></label><label><span><i>*</i>部门分配：</span><div class="editor-inline"><input value="销售部"><button type="button" class="button secondary">${icon("search")}选择</button></div></label><label><span>身份：</span><div class="editor-radios"><label><input type="radio" name="identity" checked>普通用户</label><label><input type="radio" name="identity">上级</label></div></label><label><span>头像：</span><div class="avatar-upload"><strong>＋</strong><small>上传</small></div></label><label><span>生日：</span><input type="date" name="birthday"></label><label><span>性别：</span><select name="gender"><option>请选择性别</option><option ${user.gender === "男" ? "selected" : ""}>男</option><option ${user.gender === "女" ? "selected" : ""}>女</option></select></label><label><span>邮箱：</span><input name="email" placeholder="请输入邮箱"></label><label><span>工作流引擎：</span><div class="editor-radios"><label><input type="radio" name="workflow" checked>同步</label><label><input type="radio" name="workflow">不同步</label></div></label></form></div><footer class="drawer-footer"><button type="button" class="button secondary" data-close-drawer>取消</button><button type="submit" form="systemUserEditForm" class="button primary">提交</button></footer>`;
  backdrop.hidden = false;
  drawer.classList.add("system-user-drawer");
  drawer.classList.add("open");
  drawer.setAttribute("aria-hidden", "false");
  document.body.style.overflow = "hidden";
  drawer.querySelectorAll("[data-close-drawer]").forEach(button => button.addEventListener("click", closeDrawer));
  drawer.querySelector("#systemUserEditForm").addEventListener("submit", event => { event.preventDefault(); closeDrawer(); toast("用户信息已更新"); });
}

async function openCustomer(id) {
  const customer = [...customers, ...state.poolCustomers].find(item => String(item.id) === String(id));
  if (!customer) return;
  if (state.backendOnline) {
    try {
      const customerPath = encodeURIComponent(customer.id);
      [state.customerRegistrationEvents[customer.id], state.customerAssignmentEvents[customer.id]] = await Promise.all([
        apiRequest(`/customers/${customerPath}/registration-events`),
        apiRequest(`/customers/${customerPath}/assignment-events`)
      ]);
    } catch (error) {
      console.warn("读取客户注册记录失败", error);
    }
  }
  state.view = "customers";
  state.customerDetailId = customer.id;
  state.customerDetailTab = "profile";
  ensureCurrentWorkspaceTab();
  render();
  window.scrollTo({ top: 0, behavior: "instant" });
  return;
  const drawer = document.querySelector("#detailDrawer");
  drawer.innerHTML = `<header class="drawer-header"><div><p class="eyebrow">CUSTOMER DETAIL</p><h2>客户详情</h2></div><button class="icon-button" data-close-drawer aria-label="关闭">${icon("close")}</button></header><div class="drawer-body"><div class="profile-head"><span class="person-avatar">${customer.name[0]}</span><div><h3>${customer.name}</h3><p>${customer.company}</p></div></div><div class="profile-actions"><button class="button primary" data-call-name="${customer.name}">${icon("phone")}记录通话</button><button class="button secondary" data-message-name="${customer.name}">${icon("message")}发送消息</button><button class="button secondary" data-edit-customer="${customer.id}">${icon("edit")}编辑</button></div><section class="detail-section"><h4>基本信息</h4><div class="detail-grid"><div class="detail-item"><span>手机号码</span><strong>${customer.phone}</strong></div><div class="detail-item"><span>客户编号</span><strong>${customer.id}</strong></div><div class="detail-item"><span>当前阶段</span><strong>${customerPill(customer.stage)}</strong></div><div class="detail-item"><span>客户等级</span><strong>${customer.level}</strong></div><div class="detail-item"><span>预计金额</span><strong>${money(customer.amount)}</strong></div><div class="detail-item"><span>负责人</span><strong>${customerOwnerDisplay(customer.owner)}</strong></div><div class="detail-item"><span>客户来源</span><strong>${customer.source}</strong></div><div class="detail-item"><span>所在城市</span><strong>${customer.city}</strong></div></div></section><section class="detail-section"><h4>客户备注</h4><p style="color:#475467;font-size:12px;line-height:1.8">${customer.note}</p></section><section class="detail-section"><h4>跟进安排</h4><div class="timeline"><div class="timeline-item"><time>${customer.lastContact}</time><p>最近更新客户资料与沟通状态。</p></div><div class="timeline-item"><time>${customer.nextFollow}</time><p>下一次客户跟进计划。</p></div></div></section></div>`;
  document.querySelector("#drawerBackdrop").hidden = false;
  drawer.classList.add("open");
  drawer.setAttribute("aria-hidden", "false");
  document.body.style.overflow = "hidden";
  bindDrawerEvents();
}

function openOrder(id) {
  const order = orders.find(item => item.id === id);
  if (!order) return;
  const drawer = document.querySelector("#detailDrawer");
  drawer.innerHTML = `<header class="drawer-header"><div><p class="eyebrow">ORDER DETAIL</p><h2>订单详情</h2></div><button class="icon-button" data-close-drawer aria-label="关闭">${icon("close")}</button></header><div class="drawer-body"><div class="profile-head"><span class="person-avatar">单</span><div><h3>${order.id}</h3><p>${order.customer} · ${order.product}</p></div></div><div class="profile-actions"><button class="button secondary" data-edit-order="${order.id}">${icon("edit")}编辑订单</button><button class="button secondary" data-update-payment="${order.id}">${icon("wallet")}登记回款</button><button class="button secondary" data-update-service="${order.id}">${icon("check")}更新服务</button></div><section class="detail-section"><h4>订单信息</h4><div class="detail-grid"><div class="detail-item"><span>订单金额</span><strong>${money(order.amount)}</strong></div><div class="detail-item"><span>已付金额</span><strong>${money(order.paid)}</strong></div><div class="detail-item"><span>支付状态</span><strong>${orderPill(order.status)}</strong></div><div class="detail-item"><span>服务状态</span><strong>${order.service}</strong></div><div class="detail-item"><span>业绩状态</span><strong>${order.performanceConfirmed ? "已确认" : order.paid > 0 ? "待确认" : "待回款"}</strong></div><div class="detail-item"><span>销售负责人</span><strong>${order.owner}</strong></div><div class="detail-item"><span>创建时间</span><strong>${order.created}</strong></div></div></section><section class="detail-section"><h4>订单进度</h4><div class="timeline"><div class="timeline-item"><time>${order.created}</time><p>订单已创建，当前支付状态为“${order.status}”。</p></div><div class="timeline-item"><time>当前</time><p>服务状态为“${order.service}”。</p></div></div></section>${order.performanceConfirmed ? `<div class="confirmation-state pill green">${icon("check")}业绩已确认</div>` : order.paid > 0 ? `<button class="button primary" style="width:100%;margin-top:25px" data-confirm-order="${order.id}">${icon("check")}确认业绩</button>` : `<div class="confirmation-state pill gray">${icon("clock")}登记回款后可确认业绩</div>`}</div>`;
  document.querySelector("#drawerBackdrop").hidden = false;
  drawer.classList.add("open");
  drawer.setAttribute("aria-hidden", "false");
  document.body.style.overflow = "hidden";
  bindDrawerEvents();
}

function closeDrawer() {
  const drawer = document.querySelector("#detailDrawer");
  drawer.classList.remove("open");
  drawer.classList.remove("system-user-drawer");
  drawer.setAttribute("aria-hidden", "true");
  document.querySelector("#drawerBackdrop").hidden = true;
  document.body.style.overflow = "";
  setTimeout(() => { if (!drawer.classList.contains("open")) drawer.innerHTML = ""; }, 230);
}

function toast(message, variant = "success") {
  const item = document.createElement("div");
  item.className = `toast${variant === "error" ? " toast-error" : ""}`;
  item.innerHTML = variant === "error"
    ? `<span class="toast-error-icon" aria-hidden="true">×</span><span>${message}</span>`
    : `${icon("check")}<span>${message}</span>`;
  document.querySelector("#toastRegion").append(item);
  setTimeout(() => item.remove(), 2800);
}

function callCustomer(name) {
  const customer = customers.find(item => item.name === name);
  if (!customer) { toast(`未找到客户 ${name}`); return; }
  openBusinessModal("call", customer);
}

function customerPayload(customer, overrides = {}) {
  return {
    name: customer.name,
    phone: String(customer.phone || "").replace(/\D/g, ""),
    company: customer.company,
    source: customer.source || "线上咨询",
    owner: overrides.owner ?? customer.owner ?? currentOwner(),
    stage: customer.stage || "初步沟通",
    level: customer.level || "普通客户",
    amount: Number(customer.amount || 0),
    city: customer.city || "待补充",
    note: customer.note || "暂无备注",
    nextFollowAt: customer.nextFollowAt || null,
    gender: customer.gender || "",
    birthday: customer.birthday || "",
    age: customer.age || "",
    height: customer.height || "",
    maritalStatus: customer.maritalStatus || "",
    education: customer.education || "",
    monthlyIncome: customer.monthlyIncome || "",
    annualIncome: customer.annualIncome || "",
    occupation: customer.occupation || "",
    housing: customer.housing || "",
    car: customer.car || "",
    nativePlace: customer.nativePlace || "",
    workLocation: customer.workLocation || "",
    wechat: customer.wechat || "",
    idCard: customer.idCard || "",
    remark: customer.remark || "",
    certificationStatus: customer.certificationStatus || "",
    familyStatus: customer.familyStatus || "",
    childrenStatus: customer.childrenStatus || "",
    vehicleHousing: customer.vehicleHousing || "",
    matchAgeRange: customer.matchAgeRange || "",
    matchMaritalStatus: customer.matchMaritalStatus || "",
    matchHeightRange: customer.matchHeightRange || "",
    matchEducation: customer.matchEducation || "",
    matchMonthlyIncome: customer.matchMonthlyIncome || "",
    matchMostImportant: customer.matchMostImportant || "",
    matchPersonality: customer.matchPersonality || "",
    matchChildren: customer.matchChildren || "",
    matchDealbreakers: customer.matchDealbreakers || "",
    collaborator: overrides.collaborator ?? customer.collaborator ?? "",
    tags: overrides.tags ?? customer.tags ?? []
  };
}

function mergeCustomerRecord(saved) {
  const normalized = normalizeCustomer(saved);
  const replace = list => list.some(item => item.id === normalized.id)
    ? list.map(item => item.id === normalized.id ? normalized : item)
    : [...list, normalized];
  customers = replace(customers);
  state.poolCustomers = replace(state.poolCustomers).filter(item => item.owner === "公海");
  return normalized;
}

async function setCustomerPool(customer, inPool, details = {}) {
  requireBackend();
  const saved = await apiRequest(`/customers/${encodeURIComponent(customer.id)}/pool`, { method: "PATCH", body: JSON.stringify({ inPool }) });
  const normalized = mergeCustomerRecord(saved);
  if (inPool && !isAdmin()) customers = customers.filter(item => item.id !== normalized.id);
  if (!inPool) state.poolCustomers = state.poolCustomers.filter(item => item.id !== normalized.id);
  const poolDetail = inPool
    ? `客户被移入公海${details.reason ? `，原因：${details.reason}` : ""}${details.note ? `，备注：${details.note}` : ""}`
    : `客户从公海领取，归属 ${normalized.owner}`;
  recordCustomerActivity({ customerId: normalized.id, customer: normalized.name, type: inPool ? "移入公海" : "领取客户", detail: poolDetail, owner: currentOwner() });
  return normalized;
}

async function assignCustomer(customer, owner, details = {}) {
  requireBackend();
  if (!isAdmin()) throw new Error("仅管理员可以分配客户");
  const assignmentDetails = {
    type: String(details.type || "").trim(),
    maturity: String(details.maturity || "").trim(),
    reason: String(details.reason || "").trim()
  };
  const saved = await apiRequest(`/customers/${encodeURIComponent(customer.id)}/assignment`, {
    method: "PATCH",
    body: JSON.stringify({ owner, ...assignmentDetails })
  });
  const normalized = mergeCustomerRecord(saved);
  recordCustomerActivity({
    customerId: normalized.id,
    customer: normalized.name,
    type: "分配客户",
    detail: `管理员将客户分配给 ${normalized.owner}${assignmentDetails.maturity ? `，成熟度：${assignmentDetails.maturity}` : ""}${assignmentDetails.reason ? `，原因：${assignmentDetails.reason}` : ""}`,
    owner: currentOwner()
  });
  return normalized;
}

function parseCsv(text) {
  const rows = [];
  let row = [], value = "", quoted = false;
  const source = String(text || "").replace(/^\uFEFF/, "");
  for (let index = 0; index < source.length; index += 1) {
    const char = source[index];
    if (char === '"' && quoted && source[index + 1] === '"') { value += '"'; index += 1; continue; }
    if (char === '"') { quoted = !quoted; continue; }
    if (char === "," && !quoted) { row.push(value.trim()); value = ""; continue; }
    if ((char === "\n" || char === "\r") && !quoted) {
      if (char === "\r" && source[index + 1] === "\n") index += 1;
      row.push(value.trim()); value = "";
      if (row.some(cell => cell !== "")) rows.push(row);
      row = [];
      continue;
    }
    value += char;
  }
  if (value || row.length) { row.push(value.trim()); if (row.some(cell => cell !== "")) rows.push(row); }
  return rows;
}

function parseCustomerCsv(text) {
  const rows = parseCsv(text);
  if (rows.length < 2) return [];
  const headers = rows[0].map(header => header.trim().toLowerCase());
  const aliases = {
    name: ["客户姓名", "姓名", "name"], phone: ["手机号", "手机号码", "电话号码", "phone"], company: ["公司", "所属公司", "company"],
    source: ["客户来源", "来源", "source"], owner: ["负责人", "销售负责人", "归属员工", "owner"], stage: ["跟进阶段", "阶段", "stage"],
    gender: ["性别", "gender"], birthday: ["生日", "出生日期", "birthday"], age: ["年龄", "age"], height: ["身高", "height"], maritalStatus: ["婚况", "婚姻状况", "maritalStatus"], education: ["学历", "education"], monthlyIncome: ["月收入", "monthlyIncome"], annualIncome: ["年收入", "annualIncome"], occupation: ["职业", "occupation"], housing: ["住房", "住房情况", "housing"], car: ["购车", "购车情况", "car"], nativePlace: ["籍贯", "nativePlace"], workLocation: ["工作地", "工作地点", "workLocation"], wechat: ["微信号", "wechat"], idCard: ["身份证号", "身份证号码", "idCard"], remark: ["备注说明", "remark"], certificationStatus: ["认证情况", "认证状态", "certificationStatus"], familyStatus: ["家庭情况", "familyStatus"], childrenStatus: ["子女情况", "childrenStatus"], vehicleHousing: ["房车情况", "vehicleHousing"],
    level: ["客户等级", "等级", "level"], amount: ["预计金额", "金额", "amount"], city: ["所在城市", "城市", "city"],
    note: ["备注", "客户备注", "note"], tags: ["标签", "客户标签", "tags"], nextFollowAt: ["下次跟进", "下次跟进时间", "nextFollowAt"], collaborator: ["协作人", "collaborator"]
  };
  const indexes = Object.fromEntries(Object.entries(aliases).map(([key, names]) => [key, headers.findIndex(header => names.some(name => header === name.toLowerCase()))]));
  if (indexes.phone < 0) throw new Error("未找到手机号列，请使用表头：电话号码、手机号或手机号码");
  return rows.slice(1).map(cells => Object.fromEntries(Object.entries(indexes).map(([key, index]) => [key, index >= 0 ? cells[index] || "" : ""]))).filter(row => row.name || row.phone);
}

function normalizeImportPhone(value) {
  const source = String(value ?? "").trim().replace(/[\s\u00a0-]/g, "");
  if (!source) return "";
  // Excel may expose an 11-digit phone as scientific notation.
  if (/^[+-]?\d+(?:\.\d+)?e[+-]?\d+$/i.test(source)) {
    const number = Number(source);
    if (Number.isFinite(number)) return number.toFixed(0);
  }
  return source.replace(/\D/g, "");
}

async function readCustomerImportRows(file) {
  let parsedRows;
  if (/\.(xlsx|xls)$/i.test(file.name)) {
    if (!window.XLSX) throw new Error("Excel 解析组件未加载，请检查网络后重试");
    const workbook = XLSX.read(await file.arrayBuffer(), { type: "array", cellDates: true });
    const sheet = workbook.Sheets[workbook.SheetNames[0]];
    const sheetRows = XLSX.utils.sheet_to_json(sheet, { header: 1, defval: "", raw: false });
    const csvText = sheetRows.map(row => row.map(cell => String(cell).replace(/"/g, '""')).map(cell => `"${cell}"`).join(",")).join("\n");
    parsedRows = parseCustomerCsv(csvText);
  } else {
    parsedRows = parseCustomerCsv(await file.text());
  }
  return parsedRows.map(row => ({ ...row, _importStatus: "未导入", _importMessage: "" }));
}

async function handleCustomerImportFile(event) {
  const file = event.target.files?.[0];
  if (!file) return;
  if (file.size > 50 * 1024 * 1024) { toast("文件不能超过 50MB"); return; }
  try {
    const rows = await readCustomerImportRows(file);
    const record = { id: Date.now(), uploadedAt: new Date().toLocaleString("sv-SE").replace("T", " "), status: rows.length ? "待导入" : "上传失败", count: rows.length, success: 0, fail: 0, skipped: 0, uploader: currentOwner(), fileName: file.name, rows, message: rows.length ? "" : "上传失败，请检查文件格式及字段位置是否正确" };
    state.importRows = rows;
    state.importActiveId = record.id;
    // Keep upload on the import history page; open details only on explicit click.
    state.importDetailId = null;
    state.importEditingRow = null;
    state.importSelectedRows = [];
    state.importDetailStatusFilter = "全部状态";
    state.importSkipInvalid = false;
    state.importHistory = [record, ...state.importHistory].slice(0, 100);
    persistImportHistory();
    render();
    toast(rows.length ? `已解析 ${rows.length} 条客户，请在导入详情中核对` : "文件中没有可导入的客户");
  } catch (error) {
    const record = { id: Date.now(), uploadedAt: new Date().toLocaleString("sv-SE").replace("T", " "), status: "上传失败", count: 0, success: 0, fail: 0, uploader: currentOwner(), fileName: file.name, message: "上传失败，请检查文件格式及字段位置是否正确" };
    state.importHistory = [record, ...state.importHistory].slice(0, 100);
    state.importDetailId = null;
    persistImportHistory();
    render();
    toast(`读取文件失败：${error.message}`);
  }
}

function importPayloadFromRow(row, toPool = false) {
  return {
    name: String(row.name || "").trim(),
    phone: normalizeImportPhone(row.phone),
    company: row.company || "个人客户",
    source: row.source || "线上咨询",
    owner: toPool ? "公海" : "白板",
    stage: row.stage || "初步沟通",
    level: row.level || "普通客户",
    amount: Number(row.amount || 0),
    city: row.city || "待补充",
    note: row.note || "暂无备注",
    nextFollowAt: row.nextFollowAt || null,
    gender: row.gender || "",
    birthday: row.birthday || "",
    age: row.age || "",
    height: row.height || "",
    maritalStatus: row.maritalStatus || "",
    education: row.education || "",
    monthlyIncome: row.monthlyIncome || "",
    annualIncome: row.annualIncome || "",
    occupation: row.occupation || "",
    housing: row.housing || "",
    car: row.car || "",
    nativePlace: row.nativePlace || "",
    workLocation: row.workLocation || "",
    wechat: row.wechat || "",
    idCard: row.idCard || "",
    remark: row.remark || "",
    certificationStatus: row.certificationStatus || "",
    familyStatus: row.familyStatus || "",
    childrenStatus: row.childrenStatus || "",
    vehicleHousing: row.vehicleHousing || "",
    matchAgeRange: row.matchAgeRange || "",
    matchMaritalStatus: row.matchMaritalStatus || "",
    matchHeightRange: row.matchHeightRange || "",
    matchEducation: row.matchEducation || "",
    matchMonthlyIncome: row.matchMonthlyIncome || "",
    matchMostImportant: row.matchMostImportant || "",
    matchPersonality: row.matchPersonality || "",
    matchChildren: row.matchChildren || "",
    matchDealbreakers: row.matchDealbreakers || "",
    collaborator: row.collaborator || "",
    tags: row.tags ? (Array.isArray(row.tags) ? row.tags : String(row.tags).split(/[，,]/).map(item => item.trim()).filter(Boolean)) : ["新客户"]
  };
}

async function importCustomersFromRows(rowIndexes = null) {
  const record = importHistoryRecord(state.importDetailId || state.importActiveId);
  const rows = importRowsForRecord(record);
  if (!rows.length) return;
  if (!isAdmin()) { toast("仅管理员可以审核并导入客户"); return; }
  const toPool = Boolean(document.querySelector("#importToPool")?.checked && isAdmin());
  const pendingIndexes = rows.map((row, index) => index).filter(index => !["已导入", "已跳过"].includes(rows[index]._importStatus));
  const requestedIndexes = rowIndexes === null ? pendingIndexes : rowIndexes.filter(index => pendingIndexes.includes(index));
  if (!requestedIndexes.length) {
    toast("没有可导入的客户");
    return;
  }
  const submit = document.querySelector("[data-import-all]") || document.querySelector("[data-import-selected]");
  if (submit) submit.disabled = true;
  let success = 0;
  let skipped = 0;
  let duplicates = 0;
  const failures = [];
  try {
    requireBackend();
    for (const index of requestedIndexes) {
      const row = rows[index];
      const validation = importRowValidation(row, rows);
      if (state.importSkipInvalid && validation.errors.length) {
        row._importStatus = "已跳过";
        row._importMessage = validation.errors.join("、");
        skipped += 1;
        continue;
      }
      if (validation.requiredInvalid) {
        row._importStatus = "导入失败";
        row._importMessage = validation.errors.join("、") || "客户姓名或手机号不符合要求";
        failures.push(`${row.name || `第${index + 1}条`}：${row._importMessage}`);
        continue;
      }
      try {
        const payload = importPayloadFromRow(row, toPool);
        const phone = validation.phone;
        const before = customers.concat(state.poolCustomers || []).find(item => String(item.phone || "").replace(/\D/g, "") === phone);
        const saved = await apiRequest("/customers/import", { method: "POST", body: JSON.stringify(payload) });
        const normalized = mergeCustomerRecord(saved);
        row._importStatus = "已导入";
        row._importMessage = normalized.id ? `客户ID：${normalized.id}` : "";
        if (before && Number(saved.registrationCount) > Number(before.registrationCount || 1)) duplicates += 1;
        else recordCustomerActivity({ customerId: saved.id, customer: saved.name, type: "客户导入", detail: `导入客户资料，手机号 ${saved.phone}`, owner: currentOwner() });
        success += 1;
      } catch (error) {
        row._importStatus = "导入失败";
        row._importMessage = error.message;
        failures.push(`${row.name}：${error.message}`);
      }
    }
    const completedRows = rows.filter(row => ["已导入", "已跳过"].includes(row._importStatus)).length;
    const failedRows = rows.filter(row => row._importStatus === "导入失败").length;
    record.success = rows.filter(row => row._importStatus === "已导入").length;
    record.skipped = rows.filter(row => row._importStatus === "已跳过").length;
    record.fail = failedRows;
    record.status = completedRows === rows.length && !failedRows ? "已完成" : "待导入";
    record.message = failures.join("；") || "";
    state.importRows = rows;
    state.importActiveId = record.status === "待导入" ? record.id : null;
    state.importSelectedRows = [];
    persistImportHistory();
    const summary = success || skipped ? `已处理 ${success} 条${skipped ? `，跳过 ${skipped} 条` : ""}` : "没有成功处理的客户";
    toast(`${summary}${duplicates ? `，其中重复注册 ${duplicates} 条` : ""}${toPool ? "，已进入公海" : ""}${failures.length ? `：${failures[0]}` : ""}`);
    render();
  } catch (error) {
    record.message = error.message;
    persistImportHistory();
    toast(`导入在第 ${success + skipped + 1} 条失败：${error.message}`);
  } finally {
    if (submit) submit.disabled = false;
  }
}

function downloadCustomerTemplate() {
  const csv = "客户姓名,手机号,公司,客户来源,负责人,跟进阶段,客户等级,预计金额,所在城市,备注,标签,性别,生日,年龄,身高,婚况,学历,月收入,年收入,职业,住房,购车,籍贯,工作地,微信号,身份证号,备注说明\n张三,13800138000,示例公司,线上咨询,白板,初步沟通,普通客户,0,天津,首次导入示例,新客户,男,1995-01-01,31,177cm,未婚,本科,8001-12000元,150000,工程师,已购房,已购车,天津,天津,zhangsan,120000199501010000,补充说明";
  const link = document.createElement("a"); link.href = URL.createObjectURL(new Blob(["\ufeff" + csv], { type: "text/csv;charset=utf-8" })); link.download = "优客云-客户导入模板.csv"; link.click(); setTimeout(() => URL.revokeObjectURL(link.href), 0);
  toast("客户导入模板已下载");
}

async function persistCustomerTags(customer, tags) {
  requireBackend();
  const saved = await apiRequest(`/customers/${encodeURIComponent(customer.id)}`, { method: "PUT", body: JSON.stringify(customerPayload(customer, { tags })) });
  mergeCustomerRecord(saved);
}

async function addTagToCustomer(event) {
  event.preventDefault();
  const form = event.currentTarget;
  const data = Object.fromEntries(new FormData(form));
  const customer = [...customers, ...state.poolCustomers].find(item => item.id === data.customerId);
  const tag = String(data.tag || "").trim();
  if (!customer || !tag) return;
  try {
    await persistCustomerTags(customer, [...new Set([...(customer.tags || []), tag])]);
    form.reset(); render(); toast(`已将“${tag}”添加到${customer.name}`);
  } catch (error) { toast(`标签保存失败：${error.message}`); }
}

async function renameCustomerTag(tag) {
  const next = window.prompt("请输入新的标签名称", tag)?.trim();
  if (!next || next === tag) return;
  try {
    const records = [...customers, ...state.poolCustomers].filter((item, index, all) => all.findIndex(candidate => candidate.id === item.id) === index && (isAdmin() || item.owner === currentOwner()) && (item.tags || []).includes(tag));
    for (const customer of records) await persistCustomerTags(customer, customer.tags.map(item => item === tag ? next : item));
    render(); toast(`标签“${tag}”已重命名`);
  } catch (error) { toast(`标签重命名失败：${error.message}`); }
}

async function deleteCustomerTag(tag) {
  if (!window.confirm(`确定删除标签“${tag}”吗？客户资料中的该标签也会被移除。`)) return;
  try {
    const records = [...customers, ...state.poolCustomers].filter((item, index, all) => all.findIndex(candidate => candidate.id === item.id) === index && (isAdmin() || item.owner === currentOwner()) && (item.tags || []).includes(tag));
    for (const customer of records) await persistCustomerTags(customer, customer.tags.filter(item => item !== tag));
    render(); toast(`标签“${tag}”已删除`);
  } catch (error) { toast(`标签删除失败：${error.message}`); }
}

function bindDrawerEvents() {
  document.querySelector("[data-close-drawer]")?.addEventListener("click", closeDrawer);
  document.querySelector("#detailDrawer [data-call-name]")?.addEventListener("click", event => callCustomer(event.currentTarget.dataset.callName));
  document.querySelector("#detailDrawer [data-message-name]")?.addEventListener("click", event => openConversationForCustomer(event.currentTarget.dataset.messageName));
  document.querySelector("#detailDrawer [data-edit-customer]")?.addEventListener("click", event => { const customer = customers.find(item => item.id === event.currentTarget.dataset.editCustomer); closeDrawer(); openModal(customer); });
  document.querySelector("#detailDrawer [data-edit-order]")?.addEventListener("click", event => { const order = orders.find(item => item.id === event.currentTarget.dataset.editOrder); closeDrawer(); openBusinessModal("order", order); });
  document.querySelector("#detailDrawer [data-update-payment]")?.addEventListener("click", event => { const order = orders.find(item => item.id === event.currentTarget.dataset.updatePayment); closeDrawer(); openPaymentModal(order); });
  document.querySelector("#detailDrawer [data-update-service]")?.addEventListener("click", event => { const order = orders.find(item => item.id === event.currentTarget.dataset.updateService); closeDrawer(); openServiceModal(order); });
  document.querySelector("#detailDrawer [data-confirm-order]")?.addEventListener("click", async event => {
    const order = orders.find(item => item.id === event.currentTarget.dataset.confirmOrder);
    if (!order) return;
    try {
      requireBackend();
      const result = await apiRequest(`/orders/${encodeURIComponent(order.id)}/confirmation`, { method: "PATCH", body: JSON.stringify({ confirmed: true }) });
      Object.assign(order, result && normalizeOrder(result));
      closeDrawer();
      toast("业绩已确认并写入 MySQL");
      if (state.view === "orders") render();
    } catch (error) {
      toast(`业绩确认失败：${error.message}`);
    }
  });
}

function openPaymentModal(order) {
  openBusinessModal("order-payment", order);
}

function openServiceModal(order) {
  openBusinessModal("order-service", order);
}

function exportCustomers() {
  const header = ["客户编号","姓名","手机号","公司","来源","阶段","等级","负责人","预计金额"];
  const rows = filteredCustomers().map(customer => [customer.id, customer.name, customer.phone.replaceAll(" ", ""), customer.company, customer.source, customer.stage, customer.level, customer.owner, customer.amount]);
  const csv = "\ufeff" + [header, ...rows].map(row => row.map(value => `"${String(value).replaceAll('"', '""')}"`).join(",")).join("\n");
  const url = URL.createObjectURL(new Blob([csv], { type: "text/csv;charset=utf-8" }));
  const link = document.createElement("a");
  link.href = url;
  link.download = "优客云-客户列表.csv";
  link.click();
  URL.revokeObjectURL(url);
  toast(`已导出 ${rows.length} 条客户数据`);
}

async function deleteTask(task) {
  if (!task || !window.confirm(`确定删除任务“${task.title}”吗？`)) return;
  try {
    requireBackend();
    await apiRequest(`/tasks/${task.id}`, { method: "DELETE" });
    tasks = tasks.filter(item => item.id !== task.id);
    closeBusinessModal();
    render();
    toast("任务已删除");
  } catch (error) {
    toast(`任务删除失败：${error.message}`);
  }
}

function normalizeCallDuration(value) {
  const seconds = Number(value || 0);
  if (!Number.isFinite(seconds) || seconds < 0) return 0;
  return Math.floor(seconds);
}

async function selectConversation(id) {
  state.activeConversationId = Number(id);
  try {
    if (state.backendOnline && !conversationMessages.has(state.activeConversationId)) {
      const messages = await apiRequest(`/conversations/${state.activeConversationId}/messages`);
      conversationMessages.set(state.activeConversationId, messages.map(normalizeMessage));
    }
    render();
    setTimeout(() => { const thread = document.querySelector("#messageThread"); if (thread) thread.scrollTop = thread.scrollHeight; }, 0);
  } catch (error) {
    toast(`消息加载失败：${error.message}`);
  }
}

async function openConversationForCustomer(name) {
  const customer = customers.find(item => item.name === name);
  if (!customer) return;
  try {
    requireBackend();
    let conversation = conversations.find(item => item.customerId === customer.id);
    if (!conversation) {
      conversation = normalizeConversation(await apiRequest("/conversations", { method: "POST", body: JSON.stringify({ customerId: customer.id }) }));
      conversations.unshift(conversation);
    }
    state.activeConversationId = conversation.id;
    state.messageSection = "消息管理";
    closeDrawer();
    navigate("messages");
    await selectConversation(conversation.id);
  } catch (error) {
    toast(`会话打开失败：${error.message}`);
  }
}

async function sendActiveMessage(event) {
  event.preventDefault();
  const input = document.querySelector("#messageInput");
  const content = input.value.trim();
  if (!content || !state.activeConversationId) return;
  const submit = event.currentTarget.querySelector("button[type=submit]");
  submit.disabled = true;
  try {
    requireBackend();
    const result = normalizeMessage(await apiRequest(`/conversations/${state.activeConversationId}/messages`, { method: "POST", body: JSON.stringify({ content }) }));
    const messages = conversationMessages.get(state.activeConversationId) || [];
    messages.push(result);
    conversationMessages.set(state.activeConversationId, messages);
    const conversation = conversations.find(item => item.id === state.activeConversationId);
    if (conversation) { conversation.preview = content; conversation.time = "刚刚"; }
    render();
    setTimeout(() => { const thread = document.querySelector("#messageThread"); if (thread) thread.scrollTop = thread.scrollHeight; document.querySelector("#messageInput")?.focus(); }, 0);
    toast("消息已发送并写入 MySQL");
  } catch (error) {
    toast(`消息发送失败：${error.message}`);
  } finally {
    submit.disabled = false;
  }
}

async function markAllMessagesRead() {
  try {
    requireBackend();
    await apiRequest("/conversations/read", { method: "PATCH" });
    state.unread = 0;
    conversations.forEach(item => { item.unread = 0; });
    document.querySelector("#messageBadge").hidden = true;
    render();
    toast("所有消息已标记为已读");
  } catch (error) {
    toast(`操作失败：${error.message}`);
  }
}

async function submitBusinessForm(event) {
  event.preventDefault();
  const form = event.currentTarget;
  const type = form.dataset.type;
  const data = Object.fromEntries(new FormData(form));
  const submit = form.querySelector("button[type=submit]");
  if (submit) submit.disabled = true;
  try {
    requireBackend();
    if (type === "customer-pool") {
      const customerIds = [...new Set(String(data.customerIds || "").split(/[,，\s]+/).map(value => value.trim()).filter(Boolean))];
      const selected = customerIds
        .map(id => [...customers, ...state.poolCustomers].find(customer => String(customer.id) === id))
        .filter(Boolean);
      if (!customerIds.length || selected.length !== customerIds.length) throw new Error("移入公海客户不存在，请重新选择");
      if (!data.type) throw new Error("请选择类型");
      if (!data.reason) throw new Error("请选择放弃原因");
      const details = { type: data.type, reason: data.reason, note: data.note };
      for (const customer of selected) await setCustomerPool(customer, true, details);
      state.selectedCustomerIds = [];
      closeBusinessModal();
      render();
      toast(`已将 ${selected.length} 位客户移入公海`);
    } else if (type === "customer-assignment") {
      const customerIds = [...new Set(String(data.customerIds || "").split(/[,，\s]+/).map(value => value.trim()).filter(Boolean))];
      const selected = customerIds
        .map(id => [...customers, ...state.poolCustomers].find(customer => String(customer.id) === id))
        .filter(Boolean);
      if (!customerIds.length || selected.length !== customerIds.length) throw new Error("被分配客户不存在，请重新选择");
      if (!data.owner) throw new Error("请选择接受对象");
      if (!data.type) throw new Error("请选择类型");
      if (!String(data.maturity || "").trim()) throw new Error("请填写成熟度");
      if (!String(data.reason || "").trim()) throw new Error("请填写分配原因");
      const details = { type: data.type, maturity: data.maturity, reason: data.reason };
      for (const customer of selected) await assignCustomer(customer, data.owner, details);
      state.selectedCustomerIds = [];
      closeBusinessModal();
      render();
      toast(`已将 ${selected.length} 位客户分配给 ${data.owner}`);
    } else if (type === "collaboration") {
      const customer = [...customers, ...state.poolCustomers].find(item => String(item.id) === String(data.customerId));
      const collaborators = [...form.querySelectorAll('input[name="collaborators"]')]
        .map(input => input.value.trim())
        .filter(Boolean);
      if (!customer || !collaborators.length) throw new Error("请至少增加一位协作人");
      const result = await apiRequest(`/customers/${encodeURIComponent(customer.id)}`, {
        method: "PUT",
        body: JSON.stringify(customerPayload(customer, { collaborator: collaborators.join("、") }))
      });
      const saved = mergeCustomerRecord(result);
      saved.collaborators = collaborators;
      state.customerCollaborators[customer.id] = collaborators;
      localStorage.setItem("youke.crm.customerCollaborators", JSON.stringify(state.customerCollaborators));
      closeBusinessModal();
      render();
      toast(`已保存 ${collaborators.length} 位协作人`);
    } else if (type === "task") {
      const id = data.recordId;
      const existingTask = id ? tasks.find(item => String(item.id) === String(id)) : null;
      const payload = { title: data.title, customer: data.customer, customerId: data.customerId || existingTask?.customerId || null, customerStatus: data.customerStatus || existingTask?.customerStatus || null, owner: data.owner || currentOwner(), dueAt: data.dueAt, type: data.type, priority: data.priority, completed: data.completed === "on" };
      const result = await apiRequest(id ? `/tasks/${id}` : "/tasks", { method: id ? "PUT" : "POST", body: JSON.stringify(payload) });
      const normalized = normalizeTask(result);
      const index = tasks.findIndex(item => String(item.id) === String(normalized.id));
      if (index >= 0) tasks[index] = normalized; else tasks.unshift(normalized);
      if (normalized.customerId) {
        const customer = customers.find(item => String(item.id) === String(normalized.customerId));
        if (customer) {
          customer.lastContactAt = normalized.followedAt || normalized.createdAt || new Date().toISOString();
          customer.lastContact = formatRelativeDate(customer.lastContactAt);
          if (normalized.customerStatus) customer.stage = normalized.customerStatus;
        }
      }
      if (!id && data.createNextTask === "on") {
        const nextResult = await apiRequest("/tasks", { method: "POST", body: JSON.stringify({ title: data.nextTitle, customer: data.customer, owner: data.owner || currentOwner(), dueAt: data.nextDueAt, type: "电话跟进", completed: false }) });
        tasks.unshift(normalizeTask(nextResult));
      }
      closeBusinessModal();
      render();
      toast(id ? "任务已更新" : "任务已创建");
    } else if (type === "order") {
      const id = data.recordId;
      const payload = { orderNo: data.orderNo || undefined, customer: data.customer, product: data.product, amount: Number(data.amount), paid: Number(data.paid || 0), service: data.service, owner: data.owner || currentOwner() };
      const result = await apiRequest(id ? `/orders/${encodeURIComponent(id)}` : "/orders", { method: id ? "PUT" : "POST", body: JSON.stringify(payload) });
      const normalized = normalizeOrder(result);
      const index = orders.findIndex(item => item.id === normalized.id);
      if (index >= 0) orders[index] = normalized; else orders.unshift(normalized);
      closeBusinessModal();
      render();
      toast(id ? "订单已更新" : "订单已创建");
    } else if (type === "order-payment") {
      const result = await apiRequest(`/orders/${encodeURIComponent(data.recordId)}/payment`, { method: "PATCH", body: JSON.stringify({ paid: Number(data.paid) }) });
      const order = orders.find(item => item.id === data.recordId);
      if (order) { order.paid = Number(data.paid); order.status = order.paid >= order.amount ? "已支付" : order.paid > 0 ? "部分支付" : "待支付"; if (result) Object.assign(order, normalizeOrder(result)); }
      closeBusinessModal(); render(); toast("回款已登记");
    } else if (type === "order-service") {
      const result = await apiRequest(`/orders/${encodeURIComponent(data.recordId)}/service`, { method: "PATCH", body: JSON.stringify({ status: data.service }) });
      const order = orders.find(item => item.id === data.recordId);
      if (order) { order.service = data.service; if (result) Object.assign(order, normalizeOrder(result)); }
      closeBusinessModal(); render(); toast("服务状态已更新");
    } else if (type === "refund") {
      const order = orders.find(item => item.id === data.orderId);
      const amount = Number(data.refundAmount || 0);
      if (!order || amount <= 0 || amount > order.paid) throw new Error("退款金额不能超过已回款金额");
      const refund = await apiRequest("/order-refunds", { method: "POST", body: JSON.stringify({ orderNo: order.id, amount, reason: data.refundReason.trim() }) });
      state.orderRefunds.unshift(normalizeRefund(refund));
      saveOrderRefunds();
      closeBusinessModal(); render(); toast("退款申请已提交");
    } else if (type === "call") {
      const customer = customers.find(item => item.id === data.customerId);
      const payload = { customerId: data.customerId, customer: customer?.name, direction: data.direction, status: data.status, durationSeconds: normalizeCallDuration(data.durationSeconds), note: data.note || "" };
      const result = await apiRequest("/calls", { method: "POST", body: JSON.stringify(payload) });
      calls.unshift(normalizeCall(result));
      closeBusinessModal();
      if (state.view === "calls") render();
      toast("通话记录已保存");
    } else if (type === "conversation") {
      const existing = conversations.find(item => item.customerId === data.customerId);
      const result = existing || normalizeConversation(await apiRequest("/conversations", { method: "POST", body: JSON.stringify({ customerId: data.customerId }) }));
      if (!existing) conversations.unshift(result);
      state.activeConversationId = result.id;
      conversationMessages.set(result.id, conversationMessages.get(result.id) || []);
      closeBusinessModal(); render(); toast(existing ? "已打开现有会话" : "会话已创建");
    }
  } catch (error) {
    toast(`保存失败：${error.message}`);
  } finally {
    if (submit) submit.disabled = false;
  }
}

function bindViewEvents() {
  document.querySelectorAll("[data-new-order-customer]").forEach(button => {
    if (button.textContent.includes("新订单")) button.innerHTML = `${icon("plus")}新建订单`;
  });
  document.querySelectorAll("[data-call-name]").forEach(button => { button.innerHTML = `${icon("phone")}拨打`; });
  document.querySelectorAll("[data-message-name]").forEach(button => { button.innerHTML = `${icon("message")}消息`; });
  document.querySelectorAll("[data-allocate-profile-customer]").forEach(button => { button.innerHTML = `${icon("sliders")}资源调配`; });
  document.querySelectorAll("[data-next-customer]").forEach(button => { button.innerHTML = `${icon("play")}${button.dataset.nextCustomerLabel || "下个客户"}`; });
  document.querySelectorAll("[data-customer-detail-tab]").forEach(button => button.addEventListener("click", () => { state.customerDetailTab = button.dataset.customerDetailTab; render(); }));
  document.querySelector("#dashboardFilterForm")?.addEventListener("submit", async event => {
    event.preventDefault();
    const from = document.querySelector("#dashboardFrom")?.value || "";
    const to = document.querySelector("#dashboardTo")?.value || "";
    if (from && to && to < from) { toast("结束日期不能早于开始日期"); return; }
    state.dashboardFilters = { store: document.querySelector("#dashboardStore")?.value || "", from, to };
    try { await refreshDashboard(); toast("首页数据已按筛选条件更新"); } catch (error) { toast(`查询失败：${error.message}`); }
  });
  document.querySelector("#resetDashboardFilters")?.addEventListener("click", async () => {
    state.dashboardFilters = { store: "", from: localDateValue(dashboardMonthStart), to: localDateValue(dashboardToday) };
    try { await refreshDashboard(); } catch (error) { toast(`重置失败：${error.message}`); }
  });
  document.querySelectorAll("[data-dashboard-target]").forEach(button => button.addEventListener("click", () => {
    const target = button.dataset.dashboardTarget;
    if (target === "analytics") state.analyticsSection = "邀约记录";
    if (target === "finance") state.financeSection = button.textContent.includes("退款") ? "退款明细" : "收款流水";
    navigate(target);
  }));
  document.querySelectorAll("[data-dashboard-owner]").forEach(button => button.addEventListener("click", () => {
    state.customerOwner = button.dataset.dashboardOwner;
    state.customerSection = "客户列表";
    state.customerPage = 1;
    navigate("customers");
  }));
  document.querySelectorAll("[data-edit-system-user]").forEach(button => button.addEventListener("click", event => { event.stopPropagation(); openSystemUserEditor(event.currentTarget.dataset.editSystemUser); }));
  document.querySelectorAll("[data-add-customer]").forEach(button => button.addEventListener("click", () => openModal()));
  document.querySelectorAll("[data-route]").forEach(button => button.addEventListener("click", () => navigate(button.dataset.route)));
  document.querySelectorAll("[data-subnav-value]").forEach(button => button.addEventListener("click", () => {
    if (state.view === "dashboard") { state.dashboardTab = button.dataset.subnavValue; render(); }
    if (state.view === "customers") { state.customerSection = button.dataset.subnavValue; state.customerDetailId = null; state.importDetailId = null; render(); }
    if (state.view === "tasks") {
      state.taskMode = button.dataset.subnavValue === "日历视图" ? "calendar" : button.dataset.subnavValue === "跟进记录" ? "activity" : "board";
      render();
    }
    if (state.view === "calls") { state.callSection = button.dataset.subnavValue; render(); }
    if (state.view === "messages") { state.messageSection = button.dataset.subnavValue; render(); }
    if (state.view === "orders") { state.orderSection = button.dataset.subnavValue; state.orderPage = 1; render(); }
    if (state.view === "system") { state.systemSection = button.dataset.subnavValue; render(); }
    if (state.view === "analytics") { state.analyticsSection = button.dataset.subnavValue; render(); }
    if (state.view === "finance") { state.financeSection = button.dataset.subnavValue; render(); }
  }));
  document.querySelectorAll("[data-customer-section]").forEach(button => button.addEventListener("click", () => { state.customerSection = button.dataset.customerSection; render(); }));
  document.querySelectorAll("[data-range] button").forEach(button => button.addEventListener("click", event => {
    state.range = event.currentTarget.textContent;
    render();
    toast(`数据范围已切换为${state.range}`);
  }));
  document.querySelectorAll(".task-check").forEach(button => button.addEventListener("click", async event => {
    const row = event.currentTarget.closest("[data-task-id]");
    const task = tasks.find(item => item.id === Number(row.dataset.taskId));
    const completed = !task.done;
    try {
      requireBackend();
      const saved = normalizeTask(await apiRequest(`/tasks/${task.id}/completion`, { method: "PATCH", body: JSON.stringify({ completed }) }));
      Object.assign(task, saved);
      row.classList.toggle("done", task.done);
    } catch (error) {
      toast(`任务更新失败：${error.message}`);
      return;
    }
    toast(task.done ? "任务已完成" : "任务已恢复为待办");
  }));
  document.querySelectorAll("[data-task-card]").forEach(card => card.addEventListener("click", () => { const task = tasks.find(item => String(item.id) === card.dataset.taskCard); if (task) openBusinessModal("task", task); }));
  document.querySelectorAll("[data-task-record]").forEach(button => button.addEventListener("click", event => { event.stopPropagation(); const task = tasks.find(item => String(item.id) === event.currentTarget.dataset.taskRecord); if (task) openBusinessModal("task", task); }));
  document.querySelector("#newTask")?.addEventListener("click", () => openBusinessModal("task"));
  document.querySelector("#newTaskFromRecords")?.addEventListener("click", () => openBusinessModal("task"));
  document.querySelector("#toggleTaskView")?.addEventListener("click", () => { state.taskMode = state.taskMode === "calendar" ? "board" : "calendar"; render(); });
  document.querySelectorAll("[data-calendar-shift]").forEach(button => button.addEventListener("click", () => {
    const next = new Date(state.calendarDate);
    next.setMonth(next.getMonth() + Number(button.dataset.calendarShift));
    state.calendarDate = next;
    render();
  }));
  document.querySelector("[data-calendar-today]")?.addEventListener("click", () => { state.calendarDate = new Date(); render(); });
  document.querySelector("#exportFollowUpRecords")?.addEventListener("click", exportFollowUpRecords);
  document.querySelector("#quickCall")?.addEventListener("click", () => openBusinessModal("call"));
  document.querySelector("#newCallTask")?.addEventListener("click", () => openBusinessModal("task"));
  document.querySelector("#newCallTaskEmpty")?.addEventListener("click", () => openBusinessModal("task"));
  document.querySelector("#newOrder")?.addEventListener("click", () => openBusinessModal("order"));

  const customerSearch = document.querySelector("#customerSearch");
  customerSearch?.addEventListener("input", event => { state.customerSearch = event.target.value; });
  customerSearch?.addEventListener("keydown", event => { if (event.key === "Enter") render(); });
  document.querySelector("#whiteboardNameSearch")?.addEventListener("input", event => { state.whiteboardNameSearch = event.target.value; });
  document.querySelector("#whiteboardNameSearch")?.addEventListener("keydown", event => { if (event.key === "Enter") render(); });
  document.querySelector("#whiteboardStatusFilter")?.addEventListener("change", event => { state.whiteboardStatus = event.target.value; render(); });
  const customerNameSearch = document.querySelector("#customerNameSearch");
  customerNameSearch?.addEventListener("input", event => { state.customerNameSearch = event.target.value; });
  customerNameSearch?.addEventListener("keydown", event => { if (event.key === "Enter") { state.customerPage = 1; render(); } });
  const stageFilter = document.querySelector("#stageFilter");
  stageFilter?.addEventListener("click", () => { state.customerStage = "全部阶段"; const level = document.querySelector("#levelFilter"); level?.focus(); level?.closest(".status-filter")?.classList.add("open"); });
  const levelFilter = document.querySelector("#levelFilter");
  levelFilter?.addEventListener("focus", () => levelFilter.closest(".status-filter")?.classList.add("open"));
  levelFilter?.addEventListener("input", event => {
    const value = event.target.value.trim();
    const menu = levelFilter.closest(".status-filter")?.querySelector(".status-filter-menu");
    menu?.querySelectorAll("button").forEach(button => { button.hidden = value && !button.textContent.includes(value); });
  });
  document.querySelectorAll("[data-customer-status]").forEach(button => button.addEventListener("click", () => { state.customerStatus = button.dataset.customerStatus; state.customerPage = 1; render(); }));
  const statusMenu = document.querySelector(".status-filter-menu");
  let statusScrollTimer;
  statusMenu?.addEventListener("scroll", () => {
    statusMenu.classList.add("scrolling");
    clearTimeout(statusScrollTimer);
    statusScrollTimer = setTimeout(() => statusMenu.classList.remove("scrolling"), 700);
  });
  document.querySelector("#ownerFilter")?.addEventListener("change", event => { state.customerOwner = event.target.value; render(); });
  document.querySelector("#customerOwnerToggle")?.addEventListener("click", event => {
    event.stopPropagation();
    const remove = event.target.closest("[data-customer-owner-remove]");
    if (remove) {
      state.customerOwnerSelection = customerOwnerToggleNodeSelection(state.customerOwnerSelection, remove.dataset.customerOwnerRemove);
      state.customerAdvancedOwnerSelection = JSON.parse(JSON.stringify(state.customerOwnerSelection));
      state.customerAdvancedDraftOwnerSelection = JSON.parse(JSON.stringify(state.customerOwnerSelection));
      state.customerPage = 1;
      render();
      return;
    }
    state.customerOwnerCascadeOpen = !state.customerOwnerCascadeOpen;
    state.customerAdvancedOwnerCascadeOpen = false;
    state.customerAdvancedCollaboratorCascadeOpen = false;
    state.customerDateRangePickerOpen = false;
    state.customerAdvancedDatePickerOpen = false;
    state.customerAdvancedDatePickerField = "";
    render();
  });
  document.querySelectorAll("[data-customer-owner-search]").forEach(input => input.addEventListener("input", event => {
    state.customerOwnerSearch = event.target.value;
    render();
  }));
  document.querySelectorAll("[data-customer-owner-expand]").forEach(button => button.addEventListener("click", event => {
    event.stopPropagation();
    const nodeId = button.dataset.customerOwnerExpand;
    state.customerOwnerExpandedNodes = state.customerOwnerExpandedNodes.includes(nodeId)
      ? state.customerOwnerExpandedNodes.filter(id => id !== nodeId)
      : [...state.customerOwnerExpandedNodes, nodeId];
    render();
  }));
  document.querySelectorAll("[data-customer-owner-node]").forEach(input => input.addEventListener("change", event => {
    event.stopPropagation();
    state.customerOwnerSelection = customerOwnerToggleNodeSelection(state.customerOwnerSelection, input.dataset.customerOwnerNode);
    state.customerAdvancedOwnerSelection = JSON.parse(JSON.stringify(state.customerOwnerSelection));
    state.customerAdvancedDraftOwnerSelection = JSON.parse(JSON.stringify(state.customerOwnerSelection));
    state.customerOwner = "全部负责人";
    state.customerPage = 1;
    render();
  }));
  document.querySelectorAll("[data-clear-customer-owner]").forEach(button => button.addEventListener("click", event => {
    event.stopPropagation();
    state.customerOwnerSelection = { nodes: [] };
    state.customerAdvancedOwnerSelection = { nodes: [] };
    state.customerAdvancedDraftOwnerSelection = { nodes: [] };
    state.customerOwnerSearch = "";
    state.customerOwnerCascadeOpen = true;
    state.customerPage = 1;
    render();
  }));
  document.querySelector("#sincereIdSearch")?.addEventListener("input", event => { state.customerSearch = event.target.value; });
  document.querySelector("#sincereIdSearch")?.addEventListener("keydown", event => { if (event.key === "Enter") render(); });
  document.querySelector("#openWhiteboardTags")?.addEventListener("click", () => { state.whiteboardTagModalOpen = true; render(); });
  document.querySelector("#closeWhiteboardTags")?.addEventListener("click", () => { state.whiteboardTagModalOpen = false; render(); });
  document.querySelector(".whiteboard-tag-backdrop")?.addEventListener("click", event => { if (event.target === event.currentTarget) { state.whiteboardTagModalOpen = false; render(); } });
  document.querySelectorAll("[data-whiteboard-tag]").forEach(button => button.addEventListener("click", () => { const tag = button.dataset.whiteboardTag; state.whiteboardSelectedTags = state.whiteboardSelectedTags.includes(tag) ? state.whiteboardSelectedTags.filter(item => item !== tag) : [...state.whiteboardSelectedTags, tag]; render(); }));
  document.querySelectorAll("[data-whiteboard-remove-tag]").forEach(button => button.addEventListener("click", () => { state.whiteboardSelectedTags = state.whiteboardSelectedTags.filter(tag => tag !== button.dataset.whiteboardRemoveTag); render(); }));
  document.querySelectorAll("[data-whiteboard-select-group]").forEach(checkbox => checkbox.addEventListener("change", () => { const tags = [...checkbox.closest(".whiteboard-tag-group").querySelectorAll("[data-whiteboard-tag]")].map(button => button.dataset.whiteboardTag); state.whiteboardSelectedTags = checkbox.checked ? [...new Set([...state.whiteboardSelectedTags, ...tags])] : state.whiteboardSelectedTags.filter(tag => !tags.includes(tag)); render(); }));
  document.querySelector("#clearWhiteboardTags")?.addEventListener("click", () => { state.whiteboardSelectedTags = []; render(); });
  document.querySelector("#whiteboardTagSearch")?.addEventListener("input", event => { const query = event.target.value.trim(); document.querySelectorAll("[data-whiteboard-tag]").forEach(button => { button.hidden = !!query && !button.textContent.includes(query); }); });
  const customerAdvancedButton = document.querySelector(".customer-list-page .customer-filter-actions .text-button") || document.querySelector("#openWhiteboardAdvanced");
  customerAdvancedButton?.addEventListener("click", () => {
    state.customerAdvancedDraftGender = state.customerGender;
    state.customerAdvancedDraftMaritalStatus = state.customerMaritalStatus;
    state.customerAdvancedDraftAgeMin = state.customerAgeMin;
    state.customerAdvancedDraftAgeMax = state.customerAgeMax;
    state.customerAdvancedDraftHeightMin = state.customerHeightMin;
    state.customerAdvancedDraftHeightMax = state.customerHeightMax;
    state.customerAdvancedDraftEducation = selectedCustomerEducations(state.customerEducation);
    state.customerAdvancedOwnerSelection = JSON.parse(JSON.stringify(state.customerOwnerSelection));
    state.customerAdvancedDraftOwnerSelection = JSON.parse(JSON.stringify(state.customerOwnerSelection));
    state.customerAdvancedCollaboratorSelection = JSON.parse(JSON.stringify(state.customerAdvancedCollaboratorSelection));
    state.customerAdvancedDraftCollaboratorSelection = JSON.parse(JSON.stringify(state.customerAdvancedCollaboratorSelection));
    state.customerAdvancedOwnerCascadeOpen = false;
    state.customerAdvancedCollaboratorCascadeOpen = false;
    state.customerEducationMenuOpen = false;
    state.customerAdvancedDraftDateRanges = JSON.parse(JSON.stringify(state.customerAdvancedDateRanges));
    state.customerAdvancedDatePickerOpen = false;
    state.customerAdvancedDatePickerField = "";
    state.customerAdvancedDraftUncontactedDays = state.customerUncontactedDays;
    state.customerAdvancedDraftUncontactedDaysCustom = state.customerUncontactedDaysCustom;
    state.customerAdvancedDraftDialStatus = state.customerAdvancedDialStatus;
    state.customerAdvancedDraftAvatar = state.customerAvatarFilter;
    state.customerAdvancedOpen = true;
    state.whiteboardAdvancedOpen = customerAdvancedButton.id === "openWhiteboardAdvanced";
    render();
  });
  document.querySelector("#closeWhiteboardAdvanced")?.addEventListener("click", () => { state.customerAdvancedOpen = false; state.whiteboardAdvancedOpen = false; state.customerEducationMenuOpen = false; state.customerAdvancedOwnerCascadeOpen = false; state.customerAdvancedCollaboratorCascadeOpen = false; state.customerAdvancedDatePickerOpen = false; state.customerAdvancedDatePickerField = ""; render(); });
  document.querySelector("#cancelWhiteboardAdvanced")?.addEventListener("click", () => { state.customerAdvancedOpen = false; state.whiteboardAdvancedOpen = false; state.customerEducationMenuOpen = false; state.customerAdvancedOwnerCascadeOpen = false; state.customerAdvancedCollaboratorCascadeOpen = false; state.customerAdvancedDatePickerOpen = false; state.customerAdvancedDatePickerField = ""; render(); });
  document.querySelector("#queryWhiteboardAdvanced")?.addEventListener("click", () => {
    if (state.customerAdvancedOpen) {
      const incompleteDateRange = Object.values(state.customerAdvancedDraftDateRanges).find(range => Boolean(range.start) !== Boolean(range.end));
      if (incompleteDateRange) {
        toast("请选择完整的开始日期和结束日期");
        return;
      }
      const minAgeText = String(state.customerAdvancedDraftAgeMin ?? "").trim();
      const maxAgeText = String(state.customerAdvancedDraftAgeMax ?? "").trim();
      const minAge = parseSelectedCustomerAge(minAgeText);
      const maxAge = parseSelectedCustomerAge(maxAgeText);
      if ((minAgeText && minAge === null) || (maxAgeText && maxAge === null)) {
        toast("请输入 0-150 之间的有效年龄");
        document.querySelector(minAgeText && minAge === null ? "#customerAgeMin" : "#customerAgeMax")?.focus();
        return;
      }
      if (minAge !== null && maxAge !== null && minAge > maxAge) {
        toast("最小年龄不能大于最大年龄");
        document.querySelector("#customerAgeMin")?.focus();
        return;
      }
      const minHeightText = String(state.customerAdvancedDraftHeightMin ?? "").trim();
      const maxHeightText = String(state.customerAdvancedDraftHeightMax ?? "").trim();
      const minHeight = parseSelectedCustomerHeight(minHeightText);
      const maxHeight = parseSelectedCustomerHeight(maxHeightText);
      if ((minHeightText && minHeight === null) || (maxHeightText && maxHeight === null)) {
        toast("请输入 0-300 厘米之间的有效身高");
        document.querySelector(minHeightText && minHeight === null ? "#customerHeightMin" : "#customerHeightMax")?.focus();
        return;
      }
      if (minHeight !== null && maxHeight !== null && minHeight > maxHeight) {
        toast("最小身高不能大于最大身高");
        document.querySelector("#customerHeightMin")?.focus();
        return;
      }
      const selected = state.customerAdvancedDraftUncontactedDays;
      const customDays = parseSelectedCustomerUncontactedDays(state.customerAdvancedDraftUncontactedDaysCustom);
      if (selected === "custom" && customDays === null) {
        toast("请输入有效的未联系天数");
        document.querySelector("#customerUncontactedDaysCustom")?.focus();
        return;
      }
      state.customerGender = state.customerAdvancedDraftGender;
      state.customerMaritalStatus = state.customerAdvancedDraftMaritalStatus;
      state.customerAdvancedOwnerSelection = JSON.parse(JSON.stringify(state.customerAdvancedDraftOwnerSelection));
      state.customerOwnerSelection = JSON.parse(JSON.stringify(state.customerAdvancedDraftOwnerSelection));
      state.customerOwner = "全部负责人";
      state.customerAdvancedCollaboratorSelection = JSON.parse(JSON.stringify(state.customerAdvancedDraftCollaboratorSelection));
      state.customerAdvancedDateRanges = JSON.parse(JSON.stringify(state.customerAdvancedDraftDateRanges));
      state.customerEducation = selectedCustomerEducations(state.customerAdvancedDraftEducation);
      state.customerAgeMin = minAge === null ? "" : String(minAge);
      state.customerAgeMax = maxAge === null ? "" : String(maxAge);
      state.customerHeightMin = minHeight === null ? "" : String(minHeight);
      state.customerHeightMax = maxHeight === null ? "" : String(maxHeight);
      state.customerUncontactedDays = selected;
      state.customerUncontactedDaysCustom = selected === "custom" ? String(customDays) : "";
      state.customerAdvancedDialStatus = state.customerAdvancedDraftDialStatus;
      state.customerAvatarFilter = state.customerAdvancedDraftAvatar;
      state.customerPage = 1;
      state.customerAdvancedOpen = false;
      state.whiteboardAdvancedOpen = false;
      state.customerEducationMenuOpen = false;
      state.customerAdvancedOwnerCascadeOpen = false;
      state.customerAdvancedCollaboratorCascadeOpen = false;
      state.customerAdvancedDatePickerOpen = false;
      state.customerAdvancedDatePickerField = "";
    } else {
      state.whiteboardAdvancedOpen = false;
    }
    render();
    toast("高级筛选条件已应用");
  });
  document.querySelector("#customerGenderFilter")?.addEventListener("change", event => {
    if (!state.customerAdvancedOpen) return;
    state.customerAdvancedDraftGender = event.target.value;
  });
  document.querySelector("#customerMaritalStatusFilter")?.addEventListener("change", event => {
    if (!state.customerAdvancedOpen) return;
    state.customerAdvancedDraftMaritalStatus = event.target.value;
  });
  document.querySelector("#customerAdvancedOwnerToggle")?.addEventListener("click", event => {
    event.stopPropagation();
    if (!state.customerAdvancedOpen) return;
    const remove = event.target.closest("[data-customer-advanced-owner-remove]");
    if (remove) {
      state.customerAdvancedDraftOwnerSelection = customerOwnerToggleNodeSelection(state.customerAdvancedDraftOwnerSelection, remove.dataset.customerAdvancedOwnerRemove);
      state.customerAdvancedOwnerSelection = JSON.parse(JSON.stringify(state.customerAdvancedDraftOwnerSelection));
      state.customerOwnerSelection = JSON.parse(JSON.stringify(state.customerAdvancedDraftOwnerSelection));
      state.customerPage = 1;
      render();
      return;
    }
    state.customerAdvancedOwnerCascadeOpen = !state.customerAdvancedOwnerCascadeOpen;
    state.customerOwnerCascadeOpen = false;
    state.customerAdvancedCollaboratorCascadeOpen = false;
    state.customerDateRangePickerOpen = false;
    state.customerAdvancedDatePickerOpen = false;
    state.customerAdvancedDatePickerField = "";
    render();
  });
  document.querySelectorAll("[data-customer-advanced-owner-search]").forEach(input => input.addEventListener("input", event => {
    if (!state.customerAdvancedOpen) return;
    state.customerAdvancedOwnerSearch = event.target.value;
    render();
  }));
  document.querySelectorAll("[data-customer-advanced-owner-expand]").forEach(button => button.addEventListener("click", event => {
    event.stopPropagation();
    if (!state.customerAdvancedOpen) return;
    const nodeId = button.dataset.customerAdvancedOwnerExpand;
    state.customerAdvancedOwnerExpandedNodes = state.customerAdvancedOwnerExpandedNodes.includes(nodeId)
      ? state.customerAdvancedOwnerExpandedNodes.filter(id => id !== nodeId)
      : [...state.customerAdvancedOwnerExpandedNodes, nodeId];
    render();
  }));
  document.querySelectorAll("[data-customer-advanced-owner-node]").forEach(input => input.addEventListener("change", event => {
    event.stopPropagation();
    if (!state.customerAdvancedOpen) return;
    state.customerAdvancedDraftOwnerSelection = customerOwnerToggleNodeSelection(state.customerAdvancedDraftOwnerSelection, input.dataset.customerAdvancedOwnerNode);
    state.customerAdvancedOwnerSelection = JSON.parse(JSON.stringify(state.customerAdvancedDraftOwnerSelection));
    state.customerOwnerSelection = JSON.parse(JSON.stringify(state.customerAdvancedDraftOwnerSelection));
    state.customerOwner = "全部负责人";
    state.customerPage = 1;
    render();
  }));
  document.querySelectorAll("[data-clear-customer-advanced-owner]").forEach(button => button.addEventListener("click", event => {
    event.stopPropagation();
    if (!state.customerAdvancedOpen) return;
    state.customerAdvancedDraftOwnerSelection = { nodes: [] };
    state.customerAdvancedOwnerSelection = { nodes: [] };
    state.customerOwnerSelection = { nodes: [] };
    state.customerOwner = "全部负责人";
    state.customerAdvancedOwnerSearch = "";
    state.customerAdvancedOwnerCascadeOpen = true;
    render();
  }));
  document.querySelector("#customerAdvancedCollaboratorToggle")?.addEventListener("click", event => {
    event.stopPropagation();
    if (!state.customerAdvancedOpen) return;
    const remove = event.target.closest("[data-customer-advanced-collaborator-remove]");
    if (remove) {
      state.customerAdvancedDraftCollaboratorSelection = customerOwnerToggleNodeSelection(state.customerAdvancedDraftCollaboratorSelection, remove.dataset.customerAdvancedCollaboratorRemove);
      state.customerAdvancedCollaboratorSelection = JSON.parse(JSON.stringify(state.customerAdvancedDraftCollaboratorSelection));
      state.customerAdvancedCollaboratorCascadeOpen = true;
      render();
      return;
    }
    state.customerAdvancedCollaboratorCascadeOpen = !state.customerAdvancedCollaboratorCascadeOpen;
    state.customerAdvancedOwnerCascadeOpen = false;
    state.customerDateRangePickerOpen = false;
    state.customerAdvancedDatePickerOpen = false;
    state.customerAdvancedDatePickerField = "";
    render();
  });
  document.querySelectorAll("[data-customer-advanced-collaborator-expand]").forEach(button => button.addEventListener("click", event => {
    event.stopPropagation();
    if (!state.customerAdvancedOpen) return;
    const nodeId = button.dataset.customerAdvancedCollaboratorExpand;
    state.customerAdvancedCollaboratorExpandedNodes = state.customerAdvancedCollaboratorExpandedNodes.includes(nodeId)
      ? state.customerAdvancedCollaboratorExpandedNodes.filter(id => id !== nodeId)
      : [...state.customerAdvancedCollaboratorExpandedNodes, nodeId];
    render();
  }));
  document.querySelectorAll("[data-customer-advanced-collaborator-node]").forEach(input => input.addEventListener("change", event => {
    event.stopPropagation();
    if (!state.customerAdvancedOpen) return;
    state.customerAdvancedDraftCollaboratorSelection = customerOwnerToggleNodeSelection(state.customerAdvancedDraftCollaboratorSelection, input.dataset.customerAdvancedCollaboratorNode);
    state.customerAdvancedCollaboratorSelection = JSON.parse(JSON.stringify(state.customerAdvancedDraftCollaboratorSelection));
    state.customerPage = 1;
    render();
  }));
  document.querySelectorAll("[data-open-customer-advanced-date]").forEach(button => button.addEventListener("click", event => {
    event.stopPropagation();
    const field = button.dataset.openCustomerAdvancedDate;
    const selectedRange = state.customerAdvancedDraftDateRanges[field] || { start: "", end: "" };
    state.customerAdvancedDatePickerOpen = true;
    state.customerOwnerCascadeOpen = false;
    state.customerAdvancedOwnerCascadeOpen = false;
    state.customerAdvancedDatePickerField = field;
    state.customerAdvancedDatePickerDraftStart = selectedRange.start || "";
    state.customerAdvancedDatePickerDraftEnd = selectedRange.end || "";
    state.customerAdvancedDatePickerViewMonth = customerDateRangeMonthValue(selectedRange.start || localDateValue(new Date()));
    state.customerAdvancedDatePickerPicking = selectedRange.start && !selectedRange.end ? "end" : "start";
    render();
  }));
  document.querySelectorAll("[data-customer-advanced-date-shift]").forEach(button => button.addEventListener("click", event => {
    event.stopPropagation();
    state.customerAdvancedDatePickerViewMonth = customerDateRangeShiftMonth(state.customerAdvancedDatePickerViewMonth, Number(button.dataset.customerAdvancedDateShift));
    render();
  }));
  document.querySelectorAll("[data-customer-advanced-date]").forEach(button => button.addEventListener("click", event => {
    event.stopPropagation();
    const selectedDate = button.dataset.customerAdvancedDate;
    const draftStart = state.customerAdvancedDatePickerDraftStart;
    const draftEnd = state.customerAdvancedDatePickerDraftEnd;
    if (state.customerAdvancedDatePickerPicking === "start" || !draftStart || draftEnd) {
      state.customerAdvancedDatePickerDraftStart = selectedDate;
      state.customerAdvancedDatePickerDraftEnd = "";
      state.customerAdvancedDatePickerPicking = "end";
      render();
      return;
    }
    if (selectedDate < draftStart) {
      state.customerAdvancedDatePickerDraftStart = selectedDate;
      state.customerAdvancedDatePickerDraftEnd = "";
      state.customerAdvancedDatePickerPicking = "end";
      render();
      return;
    }
    const field = state.customerAdvancedDatePickerField;
    state.customerAdvancedDraftDateRanges = { ...state.customerAdvancedDraftDateRanges, [field]: { start: draftStart, end: selectedDate } };
    state.customerAdvancedDatePickerOpen = false;
    state.customerAdvancedDatePickerField = "";
    state.customerAdvancedDatePickerDraftStart = "";
    state.customerAdvancedDatePickerDraftEnd = "";
    state.customerAdvancedDatePickerPicking = "start";
    render();
  }));
  document.querySelector("[data-clear-customer-advanced-date-range]")?.addEventListener("click", event => {
    event.stopPropagation();
    const field = state.customerAdvancedDatePickerField;
    state.customerAdvancedDraftDateRanges = { ...state.customerAdvancedDraftDateRanges, [field]: { start: "", end: "" } };
    state.customerAdvancedDatePickerDraftStart = "";
    state.customerAdvancedDatePickerDraftEnd = "";
    state.customerAdvancedDatePickerPicking = "start";
    render();
  });
  document.querySelector("#customerEducationToggle")?.addEventListener("click", event => {
    event.stopPropagation();
    if (!state.customerAdvancedOpen) return;
    state.customerEducationMenuOpen = !state.customerEducationMenuOpen;
    render();
  });
  document.querySelectorAll("[data-customer-education]").forEach(option => option.addEventListener("click", event => {
    event.stopPropagation();
    if (!state.customerAdvancedOpen) return;
    const selected = selectedCustomerEducations(state.customerAdvancedDraftEducation);
    const value = option.dataset.customerEducation;
    state.customerAdvancedDraftEducation = value === "all"
      ? []
      : selected.includes(value) ? selected.filter(item => item !== value) : [...selected, value];
    state.customerEducationMenuOpen = true;
    render();
  }));
  document.querySelectorAll("[data-remove-customer-education]").forEach(chip => chip.addEventListener("click", event => {
    event.stopPropagation();
    if (!state.customerAdvancedOpen) return;
    state.customerAdvancedDraftEducation = selectedCustomerEducations(state.customerAdvancedDraftEducation)
      .filter(item => item !== chip.dataset.removeCustomerEducation);
    render();
  }));
  document.querySelector("#customerEducationFilter")?.addEventListener("keydown", event => {
    if (event.key !== "Escape") return;
    state.customerEducationMenuOpen = false;
    render();
  });
  document.querySelector("#customerAgeMin")?.addEventListener("input", event => {
    if (!state.customerAdvancedOpen) return;
    state.customerAdvancedDraftAgeMin = event.target.value;
  });
  document.querySelector("#customerAgeMax")?.addEventListener("input", event => {
    if (!state.customerAdvancedOpen) return;
    state.customerAdvancedDraftAgeMax = event.target.value;
  });
  document.querySelector("#customerHeightMin")?.addEventListener("input", event => {
    if (!state.customerAdvancedOpen) return;
    state.customerAdvancedDraftHeightMin = event.target.value;
  });
  document.querySelector("#customerHeightMax")?.addEventListener("input", event => {
    if (!state.customerAdvancedOpen) return;
    state.customerAdvancedDraftHeightMax = event.target.value;
  });
  document.querySelector("#customerUncontactedDaysFilter")?.addEventListener("change", event => {
    state.customerAdvancedDraftUncontactedDays = event.target.value;
    render();
  });
  document.querySelector("#customerUncontactedDaysCustom")?.addEventListener("input", event => {
    state.customerAdvancedDraftUncontactedDaysCustom = event.target.value;
  });
  document.querySelector("#customerDialStatusFilter")?.addEventListener("change", event => {
    if (!state.customerAdvancedOpen) return;
    state.customerAdvancedDraftDialStatus = event.target.value;
  });
  document.querySelector("#customerAvatarFilter")?.addEventListener("change", event => {
    if (!state.customerAdvancedOpen) return;
    state.customerAdvancedDraftAvatar = event.target.value;
  });
  document.querySelector("#whiteboardApplyFilters")?.addEventListener("click", () => render());
  document.querySelector("#whiteboardResetFilters")?.addEventListener("click", () => { state.customerSearch = ""; state.whiteboardNameSearch = ""; state.whiteboardStatus = "全部"; state.whiteboardSelectedTags = []; render(); });
  document.querySelector("#openInventoryTags")?.addEventListener("click", () => { state.whiteboardTagModalOpen = true; render(); });
  document.querySelector("#openInventoryAdvanced")?.addEventListener("click", () => { state.whiteboardAdvancedOpen = true; render(); });
  document.querySelector("#openServiceAdvanced")?.addEventListener("click", () => { state.serviceAdvancedOpen = true; render(); });
  document.querySelector("#openExpiredVipAdvanced")?.addEventListener("click", () => { state.serviceAdvancedOpen = true; render(); });
  document.querySelector("#closeServiceAdvanced")?.addEventListener("click", () => { state.serviceAdvancedOpen = false; render(); });
  document.querySelector("#cancelServiceAdvanced")?.addEventListener("click", () => { state.serviceAdvancedOpen = false; render(); });
  document.querySelector("#queryServiceAdvanced")?.addEventListener("click", () => { state.serviceAdvancedOpen = false; render(); toast("高级筛选条件已应用"); });
  document.querySelector(".service-advanced-backdrop")?.addEventListener("click", event => { if (event.target === event.currentTarget) { state.serviceAdvancedOpen = false; render(); } });
  document.querySelector(".whiteboard-advanced-backdrop")?.addEventListener("click", event => {
    if (event.target === event.currentTarget) {
      state.customerAdvancedOpen = false;
      state.whiteboardAdvancedOpen = false;
      state.customerEducationMenuOpen = false;
      state.customerAdvancedOwnerCascadeOpen = false;
      state.customerAdvancedCollaboratorCascadeOpen = false;
      state.customerAdvancedDatePickerOpen = false;
      state.customerAdvancedDatePickerField = "";
      render();
    }
  });
  const dateRangeDisplay = document.querySelector(".customer-list-page:not(.pool-page) .date-range-display");
  dateRangeDisplay?.addEventListener("click", event => {
    event.preventDefault();
    const opening = !state.customerDateRangePickerOpen;
    if (opening) {
      state.customerDateRangeDraftStart = state.customerStartDate;
      state.customerDateRangeDraftEnd = state.customerEndDate;
      state.customerDateRangeViewMonth = customerDateRangeMonthValue(state.customerStartDate || localDateValue(new Date()));
      state.customerDateRangePicking = state.customerStartDate && !state.customerEndDate ? "end" : "start";
    }
    state.customerDateRangePickerOpen = opening;
    state.customerOwnerCascadeOpen = false;
    state.customerAdvancedOwnerCascadeOpen = false;
    state.customerAdvancedDatePickerOpen = false;
    state.customerAdvancedDatePickerField = "";
    render();
  });
  document.querySelectorAll("[data-customer-date-shift]").forEach(button => button.addEventListener("click", event => {
    event.stopPropagation();
    state.customerDateRangeViewMonth = customerDateRangeShiftMonth(state.customerDateRangeViewMonth, Number(button.dataset.customerDateShift));
    render();
  }));
  document.querySelectorAll("[data-customer-date]").forEach(button => button.addEventListener("click", event => {
    event.stopPropagation();
    const selectedDate = button.dataset.customerDate;
    const draftStart = state.customerDateRangeDraftStart;
    const draftEnd = state.customerDateRangeDraftEnd;
    if (state.customerDateRangePicking === "start" || !draftStart || draftEnd) {
      state.customerDateRangeDraftStart = selectedDate;
      state.customerDateRangeDraftEnd = "";
      state.customerDateRangePicking = "end";
      render();
      return;
    }
    if (selectedDate < draftStart) {
      state.customerDateRangeDraftStart = selectedDate;
      state.customerDateRangeDraftEnd = "";
      state.customerDateRangePicking = "end";
      render();
      return;
    }
    state.customerStartDate = draftStart;
    state.customerEndDate = selectedDate;
    state.customerDateRangeDraftStart = draftStart;
    state.customerDateRangeDraftEnd = selectedDate;
    state.customerDateRangePicking = "start";
    state.customerDateRangePickerOpen = false;
    state.customerPage = 1;
    render();
  }));
  document.querySelector("[data-clear-customer-date-range]")?.addEventListener("click", event => {
    event.stopPropagation();
    state.customerStartDate = "";
    state.customerEndDate = "";
    state.customerDateRangeDraftStart = "";
    state.customerDateRangeDraftEnd = "";
    state.customerDateRangePicking = "start";
    render();
  });
  document.querySelectorAll("[data-customer-scene]").forEach(button => button.addEventListener("click", () => { state.customerScene = button.dataset.customerScene; state.customerPage = 1; render(); }));
  document.querySelectorAll("[data-quick-filter]").forEach(button => button.addEventListener("click", () => { state.quickFilter = button.dataset.quickFilter; render(); }));
  document.querySelectorAll("[data-customer-scope]").forEach(button => button.addEventListener("click", () => { state.customerScope = button.dataset.customerScope; render(); }));
  document.querySelectorAll("[data-customer-sort]").forEach(button => button.addEventListener("click", event => {
    event.stopPropagation();
    const key = event.currentTarget.dataset.customerSort;
    const requestedDirection = event.target.closest("[data-customer-sort-direction]")?.dataset.customerSortDirection;
    const direction = ["asc", "desc"].includes(requestedDirection)
      ? requestedDirection
      : state.customerSort?.key === key && state.customerSort.direction === "asc" ? "desc" : "asc";
    state.customerSort = { key, direction };
    state.customerPage = 1;
    render();
  }));
  document.querySelectorAll("[data-sincere-scope]").forEach(button => button.addEventListener("click", () => { state.sincereScope = button.dataset.sincereScope; render(); }));
  document.querySelectorAll("[data-service-scope]").forEach(button => button.addEventListener("click", () => { state.serviceScope = button.dataset.serviceScope; render(); }));
  document.querySelectorAll("[data-service-scenario]").forEach(button => button.addEventListener("click", () => { state.serviceScenario = button.dataset.serviceScenario; render(); }));
  document.querySelector("#openCustomerHeader")?.addEventListener("click", () => {
    state.customerHeaderDraftColumns = [...state.customerVisibleColumns];
    state.customerHeaderModalOpen = true;
    render();
  });
  document.querySelector("#closeCustomerHeader")?.addEventListener("click", () => { state.customerHeaderModalOpen = false; render(); });
  document.querySelector("#cancelCustomerHeader")?.addEventListener("click", () => { state.customerHeaderModalOpen = false; render(); });
  document.querySelector(".customer-header-backdrop")?.addEventListener("click", event => {
    if (event.target === event.currentTarget) {
      state.customerHeaderModalOpen = false;
      render();
    }
  });
  document.querySelector("#selectAllCustomerColumns")?.addEventListener("change", event => {
    state.customerHeaderDraftColumns = event.target.checked
      ? customerTableColumnDefinitions.map(column => column.key)
      : [customerTableColumnDefinitions[0].key];
    render();
  });
  document.querySelectorAll("[data-customer-header-column]").forEach(input => input.addEventListener("change", event => {
    const key = event.currentTarget.dataset.customerHeaderColumn;
    if (event.currentTarget.checked) {
      state.customerHeaderDraftColumns = [...new Set([...state.customerHeaderDraftColumns, key])];
    } else if (state.customerHeaderDraftColumns.length <= 1) {
      toast("至少保留一个表头字段");
      render();
      return;
    } else {
      state.customerHeaderDraftColumns = state.customerHeaderDraftColumns.filter(column => column !== key);
    }
    render();
  }));
  document.querySelector("#resetCustomerHeader")?.addEventListener("click", () => {
    state.customerHeaderDraftColumns = [...defaultCustomerTableColumns];
    render();
  });
  document.querySelector("#saveCustomerHeader")?.addEventListener("click", () => {
    if (!state.customerHeaderDraftColumns.length) {
      toast("至少保留一个表头字段");
      return;
    }
    const selected = new Set([...state.customerHeaderDraftColumns, ...customerFixedColumnKeys]);
    state.customerVisibleColumns = customerTableColumnDefinitions
      .filter(column => selected.has(column.key))
      .map(column => column.key);
    localStorage.setItem("youke.crm.customerTableColumns", JSON.stringify(state.customerVisibleColumns));
    state.customerHeaderModalOpen = false;
    render();
    toast("自定义表头已保存");
  });
  document.querySelector("#applyCustomerFilters")?.addEventListener("click", () => { state.customerPage = 1; render(); });
  document.querySelector("#resetCustomerFilters")?.addEventListener("click", () => { state.customerSearch = ""; state.customerNameSearch = ""; state.customerStage = "全部阶段"; state.customerLevel = "全部等级"; state.customerStatus = "全部状态"; state.customerScene = "all"; state.customerOwner = "全部负责人"; state.customerOwnerSelection = { nodes: [] }; state.customerAdvancedCollaboratorSelection = { nodes: [] }; state.customerAdvancedDraftCollaboratorSelection = { nodes: [] }; state.customerAdvancedCollaboratorCascadeOpen = false; state.customerOwnerCascadeOpen = false; state.customerOwnerSearch = ""; state.customerOwnerExpandedNodes = ["store:youai-tianjin", "group:sales"]; state.customerStartDate = ""; state.customerEndDate = ""; state.customerDateRangePickerOpen = false; state.customerDateRangeDraftStart = ""; state.customerDateRangeDraftEnd = ""; state.customerDateRangeViewMonth = ""; state.customerDateRangePicking = "start"; state.customerGender = "all"; state.customerMaritalStatus = "all"; state.customerAgeMin = ""; state.customerAgeMax = ""; state.customerHeightMin = ""; state.customerHeightMax = ""; state.customerEducation = []; state.customerEducationMenuOpen = false; state.customerUncontactedDays = "all"; state.customerUncontactedDaysCustom = ""; state.customerAdvancedDraftGender = "all"; state.customerAdvancedDraftMaritalStatus = "all"; state.customerAdvancedDraftAgeMin = ""; state.customerAdvancedDraftAgeMax = ""; state.customerAdvancedDraftHeightMin = ""; state.customerAdvancedDraftHeightMax = ""; state.customerAdvancedDraftEducation = []; state.customerAdvancedOwnerSelection = { nodes: [] }; state.customerAdvancedDraftOwnerSelection = { nodes: [] }; state.customerAdvancedOwnerCascadeOpen = false; state.customerAdvancedOwnerSearch = ""; state.customerAdvancedOwnerExpandedNodes = ["store:youai-tianjin", "group:sales"]; state.customerAdvancedDraftDateRanges = { registration: { start: "", end: "" }, lastLogin: { start: "", end: "" }, firstAllocation: { start: "", end: "" }, lastFollowUp: { start: "", end: "" } }; state.customerAdvancedDateRanges = { registration: { start: "", end: "" }, lastLogin: { start: "", end: "" }, firstAllocation: { start: "", end: "" }, lastFollowUp: { start: "", end: "" } }; state.customerAdvancedDatePickerOpen = false; state.customerAdvancedDatePickerField = ""; state.customerAdvancedDatePickerDraftStart = ""; state.customerAdvancedDatePickerDraftEnd = ""; state.customerAdvancedDatePickerViewMonth = ""; state.customerAdvancedDatePickerPicking = "start"; state.customerAdvancedDraftUncontactedDays = "all"; state.customerAdvancedDraftUncontactedDaysCustom = ""; state.customerAdvancedDialStatus = "all"; state.customerAdvancedDraftDialStatus = "all"; state.customerAvatarFilter = "all"; state.customerAdvancedDraftAvatar = "all"; state.quickFilter = "全部客户"; state.customerPage = 1; render(); });
  document.querySelectorAll("[data-customer-page]").forEach(button => button.addEventListener("click", () => { if (!button.disabled) { state.customerPage = Number(button.dataset.customerPage); render(); } }));
  document.querySelector("#customerPageSize")?.addEventListener("change", event => { state.customerPageSize = Number(event.target.value); state.customerPage = 1; render(); });
  document.querySelectorAll("[data-select-customer]").forEach(input => input.addEventListener("change", () => { state.selectedCustomerIds = input.checked ? [...new Set([...state.selectedCustomerIds, input.dataset.selectCustomer])] : state.selectedCustomerIds.filter(id => id !== input.dataset.selectCustomer); render(); }));
  document.querySelector("#selectPageCustomers")?.addEventListener("change", event => { const ids = [...document.querySelectorAll("[data-select-customer]")].map(input => input.dataset.selectCustomer); state.selectedCustomerIds = event.target.checked ? [...new Set([...state.selectedCustomerIds, ...ids])] : state.selectedCustomerIds.filter(id => !ids.includes(id)); render(); });
  document.querySelector("#clearCustomerSelection")?.addEventListener("click", () => { state.selectedCustomerIds = []; render(); });
  document.querySelector("#batchPoolCustomers")?.addEventListener("click", () => { const selected = customers.filter(c => state.selectedCustomerIds.includes(c.id)); if (!selected.length) { toast("请先选择客户"); return; } openBusinessModal("customer-pool", { customers: selected }); });
  document.querySelector("#batchClaimCustomers")?.addEventListener("click", async () => { const selected = publicPoolCustomers().filter(c => state.selectedCustomerIds.includes(c.id)); if (!selected.length) { toast("请先选择客户"); return; } try { for (const customer of selected) await setCustomerPool(customer, false); state.selectedCustomerIds = []; render(); toast("所选客户已领取"); } catch(error) { toast(`批量领取失败：${error.message}`); } });
  document.querySelector("#customerAllocate")?.addEventListener("click", async () => {
    if (!isAdmin()) { toast("仅管理员可以分配客户"); return; }
    const selected = customers.filter(c => state.selectedCustomerIds.includes(c.id));
    if (!selected.length) { toast("请先选择客户"); return; }
    openBusinessModal("customer-assignment", { customers: selected });
  });
  document.querySelector("#batchMessageCustomers")?.addEventListener("click", () => { const selected = customers.filter(c => state.selectedCustomerIds.includes(c.id)); if (!selected.length) { toast("请先选择客户"); return; } openConversationForCustomer(selected[0].name); if (selected.length > 1) toast(`已打开首位客户会话，其余 ${selected.length - 1} 位请逐一发送`); });
  document.querySelector("#exportCustomers")?.addEventListener("click", exportCustomers);
  document.querySelector("#exportCustomerActivity")?.addEventListener("click", exportCustomerActivity);
  const customerImportFile = document.querySelector("#customerImportFile");
  if (customerImportFile) {
    customerImportFile.accept = ".xlsx,.xls,.csv,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet,application/vnd.ms-excel,text/csv";
    customerImportFile.addEventListener("change", handleCustomerImportFile);
  }
  document.querySelector("[data-import-all]")?.addEventListener("click", () => importCustomersFromRows());
  document.querySelector("[data-import-selected]")?.addEventListener("click", () => {
    const selected = state.importSelectedRows.slice();
    if (!selected.length) { toast("请先选择要导入的客户"); return; }
    importCustomersFromRows(selected);
  });
  document.querySelector("#downloadCustomerTemplate")?.addEventListener("click", downloadCustomerTemplate);
  document.querySelector("#importStatusFilter")?.addEventListener("change", event => { state.importStatusFilter = event.target.value; });
  document.querySelector("#applyImportFilters")?.addEventListener("click", render);
  document.querySelector("#resetImportFilters")?.addEventListener("click", () => { state.importStatusFilter = "全部状态"; render(); });
  document.querySelectorAll("[data-import-detail]").forEach(button => button.addEventListener("click", () => {
    const record = importHistoryRecord(button.dataset.importDetail);
    if (!record) { toast("未找到对应的导入记录"); return; }
    state.customerDetailId = null;
    state.importDetailId = record.id;
    state.importActiveId = record.status === "待导入" ? record.id : null;
    state.importRows = importRowsForRecord(record);
    state.importEditingRow = null;
    state.importSelectedRows = [];
    state.importDetailStatusFilter = "全部状态";
    state.importSkipInvalid = false;
    render();
  }));
  document.querySelector("#importDetailStatusFilter")?.addEventListener("change", event => { state.importDetailStatusFilter = event.target.value; });
  document.querySelector("#applyImportDetailFilters")?.addEventListener("click", render);
  document.querySelector("#resetImportDetailFilters")?.addEventListener("click", () => { state.importDetailStatusFilter = "全部状态"; render(); });
  document.querySelector("#importSkipInvalid")?.addEventListener("change", event => { state.importSkipInvalid = event.target.checked; render(); });
  document.querySelector("#selectImportRows")?.addEventListener("change", event => {
    const record = importHistoryRecord(state.importDetailId);
    const rows = importRowsForRecord(record);
    const pending = rows.map((row, index) => index).filter(index => !["已导入", "已跳过"].includes(rows[index]._importStatus));
    state.importSelectedRows = event.target.checked ? pending : [];
    render();
  });
  document.querySelectorAll("[data-import-select]").forEach(input => input.addEventListener("change", event => {
    const index = Number(event.currentTarget.dataset.importSelect);
    state.importSelectedRows = event.currentTarget.checked
      ? [...new Set([...state.importSelectedRows, index])]
      : state.importSelectedRows.filter(item => item !== index);
    render();
  }));
  document.querySelectorAll("[data-import-edit]").forEach(button => button.addEventListener("click", () => {
    state.importEditingRow = Number(button.dataset.importEdit);
    render();
    document.querySelector(`[data-import-row-index="${button.dataset.importEdit}"] [data-import-field="name"]`)?.focus();
  }));
  document.querySelectorAll("[data-import-save]").forEach(button => button.addEventListener("click", () => {
    const index = Number(button.dataset.importSave);
    const record = importHistoryRecord(state.importDetailId);
    const row = importRowsForRecord(record)[index];
    const tableRow = button.closest("tr");
    if (!record || !row || !tableRow) return;
    tableRow.querySelectorAll("[data-import-field]").forEach(input => { row[input.dataset.importField] = input.value.trim(); });
    row._importStatus = "未导入";
    row._importMessage = "";
    state.importRows = importRowsForRecord(record);
    state.importEditingRow = null;
    persistImportHistory();
    render();
    toast("客户信息已修改，等待导入");
  }));
  document.querySelectorAll("[data-import-cancel]").forEach(button => button.addEventListener("click", () => { state.importEditingRow = null; render(); }));
  document.querySelectorAll("[data-import-delete]").forEach(button => button.addEventListener("click", () => {
    if (!window.confirm("确定删除这条待导入客户信息吗？")) return;
    const record = importHistoryRecord(state.importDetailId);
    const index = Number(button.dataset.importDelete);
    if (!record || !Array.isArray(record.rows)) return;
    record.rows.splice(index, 1);
    record.count = record.rows.length;
    record.success = record.rows.filter(row => row._importStatus === "已导入").length;
    record.skipped = record.rows.filter(row => row._importStatus === "已跳过").length;
    record.fail = record.rows.filter(row => row._importStatus === "导入失败").length;
    record.status = record.rows.length && record.rows.every(row => ["已导入", "已跳过"].includes(row._importStatus)) ? "已完成" : "待导入";
    state.importRows = record.rows;
    state.importSelectedRows = [];
    state.importEditingRow = null;
    persistImportHistory();
    if (!record.rows.length) state.importDetailId = null;
    render();
    toast("已删除待导入记录");
  }));
  document.querySelectorAll("[data-import-row]").forEach(button => button.addEventListener("click", () => importCustomersFromRows([Number(button.dataset.importRow)])));
  document.querySelector("#tagCreateForm")?.addEventListener("submit", addTagToCustomer);
  document.querySelector("[data-add-profile-tag]")?.addEventListener("click", async event => {
    const customer = [...customers, ...state.poolCustomers].find(item => String(item.id) === String(event.currentTarget.dataset.addProfileTag));
    const tag = window.prompt("请输入客户标签")?.trim();
    if (!customer || !tag) return;
    try { await persistCustomerTags(customer, [...new Set([...(customer.tags || []), tag])]); render(); toast(`已添加标签“${tag}”`); }
    catch (error) { toast(`标签保存失败：${error.message}`); }
  });
  document.querySelectorAll("[data-rename-tag]").forEach(button => button.addEventListener("click", () => renameCustomerTag(button.dataset.renameTag)));
  document.querySelectorAll("[data-delete-tag]").forEach(button => button.addEventListener("click", () => deleteCustomerTag(button.dataset.deleteTag)));
  document.querySelectorAll("[data-release-customer]").forEach(button => button.addEventListener("click", async event => {
    event.stopPropagation();
    const customer = customers.find(item => item.id === button.dataset.releaseCustomer);
    if (!customer) return;
    openBusinessModal("customer-pool", { customers: [customer] });
  }));
  document.querySelectorAll("[data-claim-customer]").forEach(button => button.addEventListener("click", async event => {
    event.stopPropagation();
    const customer = publicPoolCustomers().find(item => item.id === button.dataset.claimCustomer);
    if (!customer) return;
    try { await setCustomerPool(customer, false); toast(`${customer.name} 已领取到我的客户`); render(); } catch (error) { toast(`领取客户失败：${error.message}`); }
  }));
  document.querySelector("[data-customer-opening]")?.addEventListener("click", () => openBusinessModal("conversation", { customer: customers.find(item => String(item.id) === String(state.customerDetailId))?.name || "" }));
  document.querySelector("[data-customer-followup]")?.addEventListener("click", () => {
    const customer = customers.find(item => String(item.id) === String(state.customerDetailId));
    openBusinessModal("task", { customer: customer?.name || "", customerId: customer?.id || "", corner: true });
  });
  document.querySelectorAll("[data-open-customer]").forEach(button => button.addEventListener("click", event => {
    const selection = window.getSelection();
    if (selection && !selection.isCollapsed && selection.toString().trim()) { event.preventDefault(); event.stopPropagation(); return; }
    event.stopPropagation();
    openCustomer(button.dataset.openCustomer);
  }));
  document.querySelector("[data-back-customer-list]")?.addEventListener("click", () => { state.customerDetailId = null; render(); });
  document.querySelectorAll("[data-edit-profile-customer]").forEach(button => button.addEventListener("click", event => { const customer = customers.find(item => String(item.id) === String(event.currentTarget.dataset.editProfileCustomer)); if (customer) openModal(customer); }));
  const profileActions = document.querySelector(".customer-profile-actions");
  const editProfile = document.querySelector("[data-edit-profile-customer]");
  if (profileActions && editProfile && isAdmin() && !profileActions.querySelector("[data-allocate-profile-customer]")) {
    editProfile.outerHTML = `<button class="button secondary" type="button" data-allocate-profile-customer="${escapeHtml(editProfile.dataset.editProfileCustomer)}">${icon("sliders")}资源调配</button><button class="button secondary" type="button" data-next-customer="${escapeHtml(editProfile.dataset.editProfileCustomer)}">${icon("chevron")}下个客户</button>`;
  }
  document.querySelector("[data-allocate-profile-customer]")?.addEventListener("click", async event => {
    const customer = [...customers, ...state.poolCustomers].find(item => String(item.id) === String(event.currentTarget.dataset.allocateProfileCustomer));
    if (!customer) return;
    openBusinessModal("customer-assignment", { customers: [customer], owner: customer.owner === "白板" ? "" : customer.owner });
  });
  document.querySelectorAll("[data-customer-more-toggle]").forEach(button => button.addEventListener("click", event => {
    event.stopPropagation();
    const menu = button.parentElement.querySelector("[data-customer-more-menu]");
    const shouldOpen = !menu.classList.contains("open");
    document.querySelectorAll(".customer-more-menu.open").forEach(item => item.classList.remove("open"));
    document.querySelectorAll("[data-customer-more-toggle]").forEach(item => item.setAttribute("aria-expanded", "false"));
    menu.classList.toggle("open", shouldOpen);
    button.setAttribute("aria-expanded", String(shouldOpen));
  }));
  document.querySelectorAll("[data-customer-more-action]").forEach(button => button.addEventListener("click", async event => {
    event.stopPropagation();
    document.querySelectorAll(".customer-more-menu.open").forEach(item => item.classList.remove("open"));
    const customer = [...customers, ...state.poolCustomers].find(item => String(item.id) === String(button.dataset.customerId));
    if (!customer) return;
    const action = Number(button.dataset.customerMoreAction);
    if (action === 0) {
      openBusinessModal("collaboration", { customer: customer.name, customerId: customer.id, collaborator: customer.collaborator || "" });
      return;
    }
    if (action === 1) { toast("转为库存功能暂未接入"); return; }
    if (action === 2) {
      if (customer.owner === "公海") { toast("该客户已在公海"); return; }
      openBusinessModal("customer-pool", { customers: [customer] });
      return;
    }
    if (action === 3) { toast("发邀请券功能暂未接入"); return; }
    if (action === 4) {
      const contactVisible = customer.contactVisible ?? (isAdmin() || customer.owner === currentOwner());
      if (!isAdmin() && !contactVisible) { toast("只有管理员或负责人可以修改客户等级"); return; }
      const libraryLabel = button.dataset.customerMoreLabel || "重点客户";
      if (customer.level === "重点客户") { toast(`该客户已是${libraryLabel}`); return; }
      try {
        requireBackend();
        const payload = customerPayload(customer);
        payload.level = "重点客户";
        const saved = await apiRequest(`/customers/${encodeURIComponent(customer.id)}`, { method: "PUT", body: JSON.stringify(payload) });
        mergeCustomerRecord(saved);
        render();
        toast(`${customer.name} 已添加至${libraryLabel}`);
      } catch (error) { toast(`添加至${libraryLabel}失败：${error.message}`); }
      return;
    }
    if (action === 5) { toast("注销用户功能暂未接入"); }
  }));
  document.querySelector("[data-next-customer]")?.addEventListener("click", event => {
    const records = state.customerSection === "白板列表" ? customers.filter(item => item.owner === "白板") : customers;
    const current = records.findIndex(item => String(item.id) === String(event.currentTarget.dataset.nextCustomer));
    const next = records.length ? records[(current + 1 + records.length) % records.length] : null;
    if (next) openCustomer(next.id);
  });
  document.querySelector("[data-new-order-customer]")?.addEventListener("click", event => { const customer = customers.find(item => String(item.id) === String(event.currentTarget.dataset.newOrderCustomer)); if (customer) openBusinessModal("order", { customer: customer.name, owner: customer.owner }); });
  document.querySelectorAll("tr[data-customer-id]").forEach(row => row.addEventListener("click", event => {
    const selection = window.getSelection();
    if (selection && !selection.isCollapsed && selection.toString().trim()) return;
    if (event.target.closest("button, input, select, textarea, label, a")) return;
    openCustomer(row.dataset.customerId);
  }));
  document.querySelectorAll("[data-resource-customer]").forEach(card => {
    card.addEventListener("click", event => { if (!event.target.closest("button")) openCustomer(card.dataset.resourceCustomer); });
    card.addEventListener("keydown", event => { if (event.key === "Enter") openCustomer(card.dataset.resourceCustomer); });
  });
  document.querySelectorAll("[data-inventory-claim]").forEach(button => button.addEventListener("click", event => { event.stopPropagation(); const customer = customers.find(item => item.id === button.dataset.inventoryClaim); toast(customer ? `${customer.name} 已领取` : "客户已领取"); }));
  document.querySelectorAll("[data-call]").forEach(button => button.addEventListener("click", event => { event.stopPropagation(); const customer = customers.find(item => item.id === button.dataset.call); callCustomer(customer.name); }));
  document.querySelectorAll("[data-call-name]").forEach(button => button.addEventListener("click", () => callCustomer(button.dataset.callName)));
  document.querySelectorAll("[data-order-detail]").forEach(button => button.addEventListener("click", event => { event.stopPropagation(); openOrder(button.dataset.orderDetail); }));
  document.querySelectorAll("tr[data-order-id]").forEach(row => row.addEventListener("click", () => openOrder(row.dataset.orderId)));
  document.querySelectorAll("[data-payment-order]").forEach(button => button.addEventListener("click", () => { const order = orders.find(item => item.id === button.dataset.paymentOrder); if (order) openPaymentModal(order); }));
  document.querySelectorAll("[data-confirm-performance]").forEach(button => button.addEventListener("click", () => confirmOrderPerformance(button.dataset.confirmPerformance)));
  document.querySelectorAll("[data-order-page]").forEach(button => button.addEventListener("click", () => { if (!button.disabled) { state.orderPage = Number(button.dataset.orderPage); render(); } }));
  document.querySelectorAll("[data-refund-review]").forEach(button => button.addEventListener("click", () => reviewRefund(button.dataset.refundReview)));
  document.querySelector("#applyOrderFilters")?.addEventListener("click", () => { state.orderKeyword = document.querySelector("#orderKeyword").value; state.orderPaymentStatus = document.querySelector("#orderPaymentFilter").value; state.orderServiceStatus = document.querySelector("#orderServiceFilter").value; state.orderPage = 1; render(); });
  document.querySelector("#resetOrderFilters")?.addEventListener("click", () => { state.orderKeyword = ""; state.orderPaymentStatus = "全部状态"; state.orderServiceStatus = "全部状态"; state.orderPage = 1; render(); });
  document.querySelector("#exportOrders")?.addEventListener("click", exportOrders);
  document.querySelector("#newContractOrder")?.addEventListener("click", () => openBusinessModal("order"));
  document.querySelector("#newPaymentRecord")?.addEventListener("click", openFirstPaymentModal);
  document.querySelector("#newPaymentRecordEmpty")?.addEventListener("click", openFirstPaymentModal);
  document.querySelector("#newRefund")?.addEventListener("click", openRefundModal);
  document.querySelector("#confirmAllPerformance")?.addEventListener("click", confirmAllPerformance);
  document.querySelector("#exportContracts")?.addEventListener("click", exportContracts);
  document.querySelector("#exportPayments")?.addEventListener("click", exportPayments);
  document.querySelector("#exportRefunds")?.addEventListener("click", exportRefunds);
  document.querySelector("#exportPerformance")?.addEventListener("click", exportPerformance);
  document.querySelector("#exportAnalytics")?.addEventListener("click", exportAnalytics);
  document.querySelector("#queryInvitations")?.addEventListener("click", () => {
    document.querySelectorAll("[data-invite-filter]").forEach(input => { state.invitationFilters[input.dataset.inviteFilter] = input.value; });
    state.invitationFilters.gender = document.querySelector('input[name="inviteGender"]:checked')?.value || "";
    state.invitationFilters.onlyFirst = Boolean(document.querySelector("#inviteOnlyFirst")?.checked);
    state.invitationPage = 1; render();
  });
  document.querySelector("#resetInvitations")?.addEventListener("click", () => {
    state.invitationFilters = { inviter: "", customer: "", customerId: "", method: "", store: "", arrivalStatus: "", createdFrom: "", createdTo: "", scheduledFrom: "", scheduledTo: "", arrivalFrom: "", arrivalTo: "", customerType: "", arrivalText: "", orderNo: "", gender: "", onlyFirst: false };
    state.invitationPage = 1; render();
  });
  document.querySelectorAll("[data-invitation-page]").forEach(button => button.addEventListener("click", () => { state.invitationPage = Number(button.dataset.invitationPage); render(); }));
  document.querySelector("#invitationPageSize")?.addEventListener("change", event => { state.invitationPageSize = Number(event.target.value); state.invitationPage = 1; render(); });
  document.querySelector("#newInvitation")?.addEventListener("click", () => { state.invitationModalOpen = true; render(); });
  document.querySelectorAll("[data-close-invitation]").forEach(button => button.addEventListener("click", () => { state.invitationModalOpen = false; render(); }));
  document.querySelector(".invitation-modal-backdrop")?.addEventListener("click", event => { if (event.target === event.currentTarget) { state.invitationModalOpen = false; render(); } });
  document.querySelector("#invitationForm")?.addEventListener("submit", async event => {
    event.preventDefault(); const data = Object.fromEntries(new FormData(event.currentTarget)); const submit = event.currentTarget.querySelector('[type="submit"]'); submit.disabled = true;
    try { requireBackend(); const saved = await apiRequest("/invitations", { method: "POST", body: JSON.stringify(data) }); invitations.unshift(normalizeInvitation(saved)); state.invitationModalOpen = false; render(); toast("邀约记录已保存到数据库"); }
    catch (error) { toast(`保存失败：${error.message}`); submit.disabled = false; }
  });
  document.querySelectorAll("[data-mark-arrival]").forEach(button => button.addEventListener("click", async () => {
    try { requireBackend(); const saved = normalizeInvitation(await apiRequest(`/invitations/${encodeURIComponent(button.dataset.markArrival)}/arrival`, { method: "PATCH", body: JSON.stringify({ arrivalAt: new Date().toISOString().slice(0, 19) }) })); const index = invitations.findIndex(row => row.id === saved.id); if (index >= 0) invitations[index] = saved; render(); toast("到店状态已更新"); }
    catch (error) { toast(`登记失败：${error.message}`); }
  }));
  document.querySelector("#queryLedgers")?.addEventListener("click", () => { state.ledgerFilters = { status: document.querySelector("#ledgerStatus").value, store: document.querySelector("#ledgerStore").value, from: document.querySelector("#ledgerFrom").value, to: document.querySelector("#ledgerTo").value }; render(); });
  document.querySelector("#resetLedgers")?.addEventListener("click", () => { state.ledgerFilters = { status: "", store: "", from: "", to: "" }; render(); });
  document.querySelector("#newLedger")?.addEventListener("click", () => { state.ledgerModalOpen = true; render(); });
  document.querySelectorAll("[data-close-ledger]").forEach(button => button.addEventListener("click", () => { state.ledgerModalOpen = false; render(); }));
  document.querySelector(".ledger-modal-backdrop")?.addEventListener("click", event => { if (event.target === event.currentTarget) { state.ledgerModalOpen = false; render(); } });
  document.querySelector("#ledgerOrderNo")?.addEventListener("change", event => { const option = event.target.selectedOptions[0]; const amount = document.querySelector("#ledgerAmount"); const beneficiary = document.querySelector("#ledgerBeneficiary"); if (option?.dataset.paid) amount.value = (Number(option.dataset.paid) * .2).toFixed(2); if (option?.dataset.owner) beneficiary.value = `${option.dataset.owner} · 渠道账户`; });
  document.querySelector("#ledgerForm")?.addEventListener("submit", async event => { event.preventDefault(); const submit=event.currentTarget.querySelector('[type="submit"]'); submit.disabled=true; try { const saved=normalizeLedger(await apiRequest("/ledger-accounts",{method:"POST",body:JSON.stringify(Object.fromEntries(new FormData(event.currentTarget)))})); ledgerAccounts.unshift(saved); state.ledgerModalOpen=false; render(); toast("分账记录已创建并写入数据库"); } catch(error){ toast(`创建失败：${error.message}`); submit.disabled=false; } });
  document.querySelectorAll("[data-execute-ledger]").forEach(button => button.addEventListener("click", async () => { if(!window.confirm("确认执行这笔分账吗？")) return; try { const saved=normalizeLedger(await apiRequest(`/ledger-accounts/${encodeURIComponent(button.dataset.executeLedger)}/execute`,{method:"PATCH"})); const index=ledgerAccounts.findIndex(row=>row.id===saved.id); if(index>=0) ledgerAccounts[index]=saved; render(); toast("分账执行成功"); } catch(error){ toast(`执行失败：${error.message}`); } }));

  document.querySelectorAll("[data-conversation]").forEach(item => item.addEventListener("click", () => selectConversation(item.dataset.conversation)));
  document.querySelector("#newConversation")?.addEventListener("click", () => openBusinessModal("conversation"));
  document.querySelector("#conversationSearch")?.addEventListener("input", event => { const keyword = event.target.value.trim().toLowerCase(); document.querySelectorAll("[data-conversation]").forEach(item => { item.hidden = Boolean(keyword) && !item.textContent.toLowerCase().includes(keyword); }); });
  document.querySelector("#messageForm")?.addEventListener("submit", sendActiveMessage);
  document.querySelector("#messageInput")?.addEventListener("keydown", event => { if (event.key === "Enter" && !event.shiftKey) { event.preventDefault(); document.querySelector("#messageForm").requestSubmit(); } });
  document.querySelector("#markAllRead")?.addEventListener("click", markAllMessagesRead);
  document.querySelectorAll("[data-notification-filter]").forEach(button => button.addEventListener("click", () => { state.notificationFilter = button.dataset.notificationFilter; render(); }));
  document.querySelector("#markAllSystemNotifications")?.addEventListener("click", markAllSystemNotificationsRead);
  document.querySelector("#exportSystemNotifications")?.addEventListener("click", exportSystemNotifications);
  document.querySelectorAll("[data-open-notification]").forEach(button => button.addEventListener("click", async event => {
    const row = systemNotificationRows().find(item => item.id === event.currentTarget.dataset.openNotification);
    if (row) await openSystemNotification(row);
  }));
  document.querySelector("#messageTemplateForm")?.addEventListener("submit", submitMessageTemplate);
  document.querySelector("#resetMessageTemplate")?.addEventListener("click", () => { state.editingTemplateId = null; render(); });
  document.querySelector("#cancelMessageTemplateEdit")?.addEventListener("click", () => { state.editingTemplateId = null; render(); });
  document.querySelector("#messageTemplateSearch")?.addEventListener("input", event => { state.messageTemplateSearch = event.target.value; });
  document.querySelector("#messageTemplateSearch")?.addEventListener("keydown", event => { if (event.key === "Enter") render(); });
  document.querySelectorAll("[data-edit-message-template]").forEach(button => button.addEventListener("click", () => editMessageTemplate(button.dataset.editMessageTemplate)));
  document.querySelectorAll("[data-delete-message-template]").forEach(button => button.addEventListener("click", () => deleteMessageTemplate(button.dataset.deleteMessageTemplate)));
  document.querySelectorAll("[data-use-message-template]").forEach(button => button.addEventListener("click", () => useMessageTemplate(button.dataset.useMessageTemplate)));
  document.querySelectorAll("[data-call-filter]").forEach(button => button.addEventListener("click", () => { state.callFilter = button.dataset.callFilter; render(); }));
  document.querySelectorAll("[data-call-task-filter]").forEach(button => button.addEventListener("click", () => { state.callTaskFilter = button.dataset.callTaskFilter; render(); }));
  document.querySelector("#callAgentFilter")?.addEventListener("change", event => { state.callAgentFilter = event.target.value; render(); });
  document.querySelector("#callAiPreview")?.addEventListener("change", event => { state.callAiPreview = event.target.checked; render(); });
  document.querySelector("#applyCallFilters")?.addEventListener("click", () => { state.callKeyword = document.querySelector("#callKeyword")?.value || ""; state.callNameKeyword = document.querySelector("#callNameKeyword")?.value || ""; state.callAgentFilter = document.querySelector("#callAgentFilter")?.value || "全部坐席"; state.callDirectionFilter = document.querySelector("#callDirectionFilter")?.value || "全部"; state.callStatusFilter = document.querySelector("#callStatusFilter")?.value || "全部"; render(); });
  document.querySelectorAll("#callKeyword, #callNameKeyword").forEach(input => input.addEventListener("keydown", event => { if (event.key === "Enter") document.querySelector("#applyCallFilters")?.click(); }));
  document.querySelector("#resetCallFilters")?.addEventListener("click", () => { state.callKeyword = ""; state.callNameKeyword = ""; state.callAgentFilter = "全部坐席"; state.callDirectionFilter = "全部"; state.callStatusFilter = "全部"; render(); });
  document.querySelector("#querySeatAgents")?.addEventListener("click", () => toast("坐席筛选已应用"));
  document.querySelector("#resetSeatAgents")?.addEventListener("click", () => { const status = document.querySelector("#seatStatusFilter"); const agent = document.querySelector("#seatAgentFilter"); if (status) status.selectedIndex = 0; if (agent) agent.selectedIndex = 0; });
  document.querySelectorAll("[data-seat-monitor]").forEach(button => button.addEventListener("click", () => toast(`${button.dataset.seatMonitor} 当前离线，暂时无法监听`)));
  document.querySelectorAll("[data-call-review]").forEach(button => button.addEventListener("click", () => toggleCallReview(button.dataset.callReview)));
  document.querySelector("#exportCallTasks")?.addEventListener("click", exportCallTasks);
  document.querySelector("#exportCallAgents")?.addEventListener("click", exportCallAgents);
  document.querySelector("#exportCallQuality")?.addEventListener("click", exportCallQuality);
  document.querySelector("#exportCalls")?.addEventListener("click", exportCalls);
}

function exportOrders() {
  const rows = filteredOrders();
  const header = ["订单号", "客户", "商品/套餐", "订单金额", "已付金额", "支付状态", "服务状态", "负责人", "创建时间"];
  const csv = "\ufeff" + [header, ...rows.map(order => [order.id, order.customer, order.product, order.amount, order.paid, order.status, order.service, order.owner, order.created])].map(row => row.map(value => `"${String(value ?? "").replaceAll('"', '""')}"`).join(",")).join("\n");
  const link = document.createElement("a"); link.href = URL.createObjectURL(new Blob([csv], { type: "text/csv;charset=utf-8" })); link.download = "优客云-订单列表.csv"; link.click(); setTimeout(() => URL.revokeObjectURL(link.href), 0);
  toast(`已导出 ${rows.length} 条订单数据`);
}

function openFirstPaymentModal() {
  const order = orders.find(item => item.amount > item.paid) || orders[0];
  if (order) openPaymentModal(order); else toast("暂无可登记回款的订单");
}

function openRefundModal() {
  const order = orders.find(item => item.paid > 0);
  if (!order) { toast("暂无可申请退款的已回款订单"); return; }
  openBusinessModal("refund", order);
}

async function reviewRefund(id) {
  const refund = state.orderRefunds.find(item => item.id === id);
  if (!refund) return;
  const result = window.prompt(`审核退款：${refund.amount} 元，${refund.reason}\n请输入“通过”或“拒绝”`, "通过");
  if (!result) return;
  const decision = result.trim();
  if (!["通过", "拒绝"].includes(decision)) { toast("审核结果只能填写“通过”或“拒绝”"); return; }
  const status = decision === "通过" ? "已通过" : "已拒绝";
  try {
    requireBackend();
    const saved = await apiRequest(`/order-refunds/${encodeURIComponent(id)}/review`, { method: "PATCH", body: JSON.stringify({ status }) });
    Object.assign(refund, normalizeRefund(saved));
    saveOrderRefunds(); render(); toast(`退款申请已标记为${refund.status}`);
  } catch (error) { toast(`退款审核失败：${error.message}`); }
}

async function confirmOrderPerformance(id) {
  const order = orders.find(item => item.id === id);
  if (!order) return;
  if (order.paid <= 0) { toast("订单尚未回款，暂不能确认业绩"); return; }
  try {
    requireBackend();
    const result = await apiRequest(`/orders/${encodeURIComponent(id)}/confirmation`, { method: "PATCH", body: JSON.stringify({ confirmed: true }) });
    Object.assign(order, normalizeOrder(result));
    render(); toast("业绩已确认并写入 MySQL");
  } catch (error) { toast(`业绩确认失败：${error.message}`); }
}

async function confirmAllPerformance() {
  const pending = orders.filter(order => !order.performanceConfirmed && order.paid > 0);
  for (const order of pending) await confirmOrderPerformance(order.id);
  if (pending.length) toast(`已确认 ${pending.length} 个订单业绩`);
}

function exportContracts() {
  const rows = orderRowsForSection();
  const header = ["合同编号", "客户", "产品/套餐", "合同金额", "已回款", "回款状态", "履约状态", "销售", "创建时间"];
  const csv = "\ufeff" + [header, ...rows.map(order => [order.id, order.customer, order.product, order.amount, order.paid, order.status, order.service, order.owner, order.created])].map(row => row.map(value => `"${String(value ?? "").replaceAll('"', '""')}"`).join(",")).join("\n");
  const link = document.createElement("a"); link.href = URL.createObjectURL(new Blob([csv], { type: "text/csv;charset=utf-8" })); link.download = "优客云-合同列表.csv"; link.click(); setTimeout(() => URL.revokeObjectURL(link.href), 0); toast("合同列表已导出");
}

function exportPayments() {
  const rows = orderRowsForSection().filter(order => order.paid > 0);
  const header = ["订单号", "客户", "订单金额", "累计回款", "剩余应收", "支付状态"];
  const csv = "\ufeff" + [header, ...rows.map(order => [order.id, order.customer, order.amount, order.paid, Math.max(0, order.amount - order.paid), order.status])].map(row => row.map(value => `"${String(value ?? "").replaceAll('"', '""')}"`).join(",")).join("\n");
  const link = document.createElement("a"); link.href = URL.createObjectURL(new Blob([csv], { type: "text/csv;charset=utf-8" })); link.download = "优客云-回款记录.csv"; link.click(); setTimeout(() => URL.revokeObjectURL(link.href), 0); toast("回款记录已导出");
}

function exportRefunds() {
  const header = ["退款编号", "订单号", "客户", "退款金额", "退款原因", "申请人", "状态"];
  const csv = "\ufeff" + [header, ...state.orderRefunds.map(refund => [refund.id, refund.orderId, refund.customer, refund.amount, refund.reason, refund.applicant, refund.status])].map(row => row.map(value => `"${String(value ?? "").replaceAll('"', '""')}"`).join(",")).join("\n");
  const link = document.createElement("a"); link.href = URL.createObjectURL(new Blob([csv], { type: "text/csv;charset=utf-8" })); link.download = "优客云-退款记录.csv"; link.click(); setTimeout(() => URL.revokeObjectURL(link.href), 0); toast("退款记录已导出");
}

function exportPerformance() {
  const header = ["订单号", "客户", "销售", "订单金额", "已回款", "支付状态", "业绩状态"];
  const csv = "\ufeff" + [header, ...orderRowsForSection().map(order => [order.id, order.customer, order.owner, order.amount, order.paid, order.status, order.performanceConfirmed ? "已确认" : "待确认"])].map(row => row.map(value => `"${String(value ?? "").replaceAll('"', '""')}"`).join(",")).join("\n");
  const link = document.createElement("a"); link.href = URL.createObjectURL(new Blob([csv], { type: "text/csv;charset=utf-8" })); link.download = "优客云-业绩确认.csv"; link.click(); setTimeout(() => URL.revokeObjectURL(link.href), 0); toast("业绩确认报表已导出");
}

function exportCalls() {
  const header = ["客户", "手机号", "方向", "状态", "通话时长(秒)", "坐席", "开始时间", "备注"];
  const csv = "\ufeff" + [header, ...calls.map(call => [call.customer, call.phone, call.direction, call.status, call.durationSeconds, call.agent, call.started, call.note || ""])].map(row => row.map(value => `"${String(value ?? "").replaceAll('"', '""')}"`).join(",")).join("\n");
  const link = document.createElement("a"); link.href = URL.createObjectURL(new Blob([csv], { type: "text/csv;charset=utf-8" })); link.download = "优客云-通话记录.csv"; link.click(); setTimeout(() => URL.revokeObjectURL(link.href), 0);
  toast(`已导出 ${calls.length} 条通话记录`);
}

function exportCallTasks() {
  const rows = tasks.filter(task => task.type === "电话跟进");
  const header = ["任务", "客户", "负责人", "截止时间", "优先级", "状态"];
  const csv = "\ufeff" + [header, ...rows.map(task => [task.title, task.customer, task.owner, task.due, task.priority, task.done ? "已完成" : "待处理"])].map(row => row.map(value => `"${String(value ?? "").replaceAll('"', '""')}"`).join(",")).join("\n");
  const link = document.createElement("a"); link.href = URL.createObjectURL(new Blob([csv], { type: "text/csv;charset=utf-8" })); link.download = "优客云-通话任务.csv"; link.click(); setTimeout(() => URL.revokeObjectURL(link.href), 0);
  toast(`已导出 ${rows.length} 条通话任务`);
}

function callAgentRows() {
  const agents = [...new Set([...calls.map(call => call.agent || call.owner), ...tasks.map(task => task.owner)].filter(Boolean))];
  return agents.map(agent => {
    const agentCalls = calls.filter(call => (call.agent || call.owner) === agent);
    const agentTasks = tasks.filter(task => task.owner === agent);
    const connected = agentCalls.filter(call => call.status === "已接通").length;
    return [agent, agentCalls.length, connected, agentCalls.length ? `${Math.round(connected / agentCalls.length * 100)}%` : "0%", formatDuration(agentCalls.reduce((sum, call) => sum + call.durationSeconds, 0)), agentTasks.length, agentTasks.length ? `${Math.round(agentTasks.filter(task => task.done).length / agentTasks.length * 100)}%` : "0%"]; 
  });
}

function exportCallAgents() {
  const header = ["坐席", "通话数", "接通数", "接通率", "通话时长", "跟进任务", "任务完成率"];
  const csv = "\ufeff" + [header, ...callAgentRows()].map(row => row.map(value => `"${String(value ?? "").replaceAll('"', '""')}"`).join(",")).join("\n");
  const link = document.createElement("a"); link.href = URL.createObjectURL(new Blob([csv], { type: "text/csv;charset=utf-8" })); link.download = "优客云-坐席报表.csv"; link.click(); setTimeout(() => URL.revokeObjectURL(link.href), 0);
  toast("坐席报表已导出");
}

function exportCallQuality() {
  const header = ["客户", "坐席", "通话结果", "通话时长", "备注", "录音", "质检状态"];
  const csv = "\ufeff" + [header, ...calls.map(call => [call.customer, call.agent || call.owner, call.status, formatDuration(call.durationSeconds), call.note || "暂无备注", "暂无录音", state.callReviews[call.id] ? "已完成" : "待质检"])].map(row => row.map(value => `"${String(value ?? "").replaceAll('"', '""')}"`).join(",")).join("\n");
  const link = document.createElement("a"); link.href = URL.createObjectURL(new Blob([csv], { type: "text/csv;charset=utf-8" })); link.download = "优客云-录音质检.csv"; link.click(); setTimeout(() => URL.revokeObjectURL(link.href), 0);
  toast("录音质检结果已导出");
}

function exportAnalytics() {
  if (state.analyticsSection === "客户分析") {
    const header = ["客户", "公司", "来源", "阶段", "等级", "负责人", "预计金额"];
    const csv = "\ufeff" + [header, ...customers.map(customer => [customer.name, customer.company, customer.source, customer.stage, customer.level, customer.owner, customer.amount])].map(row => row.map(value => `"${String(value ?? "").replaceAll('"', '""')}"`).join(",")).join("\n");
    const link = document.createElement("a"); link.href = URL.createObjectURL(new Blob([csv], { type: "text/csv;charset=utf-8" })); link.download = `优客云-客户分析-${state.range}.csv`; link.click(); setTimeout(() => URL.revokeObjectURL(link.href), 0); toast("客户分析已导出"); return;
  }
  if (state.analyticsSection === "销售分析") {
    const rows = orders.filter(order => order.status !== "已取消");
    const header = ["订单号", "客户", "负责人", "订单金额", "已回款", "支付状态", "业绩状态"];
    const csv = "\ufeff" + [header, ...rows.map(order => [order.id, order.customer, order.owner, order.amount, order.paid, order.status, order.performanceConfirmed ? "已确认" : "待确认"])].map(row => row.map(value => `"${String(value ?? "").replaceAll('"', '""')}"`).join(",")).join("\n");
    const link = document.createElement("a"); link.href = URL.createObjectURL(new Blob([csv], { type: "text/csv;charset=utf-8" })); link.download = `优客云-销售分析-${state.range}.csv`; link.click(); setTimeout(() => URL.revokeObjectURL(link.href), 0); toast("销售分析已导出"); return;
  }
  if (state.analyticsSection === "团队分析") {
    const names = [...new Set([...customers.map(item => item.owner), ...orders.map(item => item.owner), ...tasks.map(item => item.owner)].filter(Boolean))];
    const header = ["成员", "客户数", "有效订单", "订单金额", "已回款", "任务数", "已完成任务"];
    const rows = names.map(owner => { const ownerOrders = orders.filter(item => item.owner === owner && item.status !== "已取消"); const ownerTasks = tasks.filter(item => item.owner === owner); return [owner, customers.filter(item => item.owner === owner).length, ownerOrders.length, ownerOrders.reduce((sum, item) => sum + item.amount, 0), ownerOrders.reduce((sum, item) => sum + item.paid, 0), ownerTasks.length, ownerTasks.filter(item => item.done).length]; });
    const csv = "\ufeff" + [header, ...rows].map(row => row.map(value => `"${String(value ?? "").replaceAll('"', '""')}"`).join(",")).join("\n");
    const link = document.createElement("a"); link.href = URL.createObjectURL(new Blob([csv], { type: "text/csv;charset=utf-8" })); link.download = `优客云-团队分析-${state.range}.csv`; link.click(); setTimeout(() => URL.revokeObjectURL(link.href), 0); toast("团队分析已导出"); return;
  }
  const summary = analyticsSummary();
  const rows = [["销售目标", `${summary.target}%`], ["新增商机", summary.opportunities], ["商机金额(万元)", summary.pipeline], ["赢单金额(万元)", summary.won], ["客单价(万元)", summary.average], ["平均周期(天)", summary.cycle]];
  const csv = "\ufeff指标,当前值\n" + rows.map(row => row.map(value => `"${String(value).replaceAll('"', '""')}"`).join(",")).join("\n");
  const link = document.createElement("a"); link.href = URL.createObjectURL(new Blob([csv], { type: "text/csv;charset=utf-8" })); link.download = `优客云-经营报表-${state.range}.csv`; link.click(); setTimeout(() => URL.revokeObjectURL(link.href), 0);
  toast("经营报表已导出");
}

function escapeHtml(value) {
  const element = document.createElement("div");
  element.textContent = value;
  return element.innerHTML;
}

document.querySelectorAll(".nav-item").forEach(item => item.addEventListener("click", event => {
  event.preventDefault();
  if (item.dataset.view === "dashboard") {
    navigate("dashboard");
    return;
  }
  showNavHoverMenu(item);
}));
document.querySelector("#workspaceTabs").addEventListener("click", event => {
  const closeButton = event.target.closest("[data-close-workspace-tab]");
  if (closeButton) {
    event.stopPropagation();
    closeWorkspaceTab(closeButton.dataset.closeWorkspaceTab);
    return;
  }
  const tab = event.target.closest("[data-workspace-tab]");
  if (tab) activateWorkspaceTab(tab.dataset.workspaceTab);
});
document.querySelector("#workspaceTabs").addEventListener("keydown", event => {
  if (event.key !== "Enter" && event.key !== " ") return;
  const tab = event.target.closest("[data-workspace-tab]");
  if (!tab || event.target.closest("[data-close-workspace-tab]")) return;
  event.preventDefault();
  activateWorkspaceTab(tab.dataset.workspaceTab);
});

const navHoverItems = {
  dashboard: [],
  customers: ["客户列表", "公海列表", "诚意资源", "白板列表", "库存资源", "服务库", "过期VIP库", "客户导入"],
  tasks: ["任务看板", "日历视图", "跟进记录"],
  calls: ["通话记录", "坐席管理"],
  messages: ["消息管理", "短信记录"],
  orders: ["订单列表", "合同列表", "流水列表", "业绩上传", "退费列表", "业绩列表", "卡券管理"],
  system: ["用户管理", "菜单管理", "部门管理", "业务设置"],
  analytics: ["邀约记录", "通话沟通记录", "分账记录", "库存记录", "消息统计", "通话统计", "约会列表"],
  finance: ["财务概览", "收款流水", "退款明细"]
};
const navHoverMenu = document.createElement("div");
navHoverMenu.className = "nav-hover-menu";
document.body.appendChild(navHoverMenu);
let navHoverTimer;
function showNavHoverMenu(item) {
  const items = navHoverItems[item.dataset.view] || [];
  if (!items.length) {
    navHoverMenu.classList.remove("open");
    return;
  }
  navHoverMenu.dataset.view = item.dataset.view;
  const activeSection = currentWorkspaceSection(item.dataset.view);
  navHoverMenu.innerHTML = items.map(label => `<button type="button" class="${label === activeSection ? "active" : ""}" data-hover-subnav="${escapeHtml(label)}">${escapeHtml(label)}</button>`).join("");
  const rect = item.getBoundingClientRect();
  navHoverMenu.style.left = `${Math.max(8, rect.left)}px`;
  navHoverMenu.style.top = `${rect.bottom + 4}px`;
  navHoverMenu.classList.add("open");
}
function scheduleNavHoverHide() { navHoverTimer = setTimeout(() => navHoverMenu.classList.remove("open"), 120); }
document.querySelectorAll(".nav-item").forEach(item => {
  item.addEventListener("mouseenter", () => { clearTimeout(navHoverTimer); showNavHoverMenu(item); });
  item.addEventListener("mouseleave", scheduleNavHoverHide);
});
navHoverMenu.addEventListener("mouseenter", () => clearTimeout(navHoverTimer));
navHoverMenu.addEventListener("mouseleave", scheduleNavHoverHide);
navHoverMenu.addEventListener("click", event => {
  const button = event.target.closest("[data-hover-subnav]");
  if (!button) return;
  const label = button.dataset.hoverSubnav;
  const view = navHoverMenu.dataset.view;
  if (view === "dashboard") state.dashboardTab = label;
  if (view === "customers") state.customerSection = label;
  if (view === "tasks") state.taskMode = label === "日历视图" ? "calendar" : label === "跟进记录" ? "activity" : "board";
  if (view === "calls") state.callSection = label;
  if (view === "messages") state.messageSection = label;
  if (view === "orders") state.orderSection = label;
  if (view === "system") state.systemSection = label;
  if (view === "analytics") state.analyticsSection = label;
  if (view === "finance") state.financeSection = label;
  navigate(view);
  navHoverMenu.classList.remove("open");
});
document.querySelector("#mobileMenuButton").addEventListener("click", () => document.querySelector("#primaryNav").classList.toggle("open"));
document.querySelector("#closeModal").addEventListener("click", closeModal);
document.querySelector("#cancelModal").addEventListener("click", closeModal);
document.querySelector("#modalBackdrop").addEventListener("click", event => { if (event.target.id === "modalBackdrop") closeModal(); });
document.querySelector("#closeBusinessModal").addEventListener("click", closeBusinessModal);
document.querySelector("#businessModalBackdrop").addEventListener("click", event => { if (event.target.id === "businessModalBackdrop") closeBusinessModal(); });
document.querySelector("#drawerBackdrop").addEventListener("click", closeDrawer);
document.querySelector("#customerForm").addEventListener("submit", async event => {
  event.preventDefault();
  const data = Object.fromEntries(new FormData(event.currentTarget));
  const existing = data.customerId ? customers.find(customer => customer.id === data.customerId) : null;
  const payload = {
    name: data.name,
    phone: data.phone.replace(/\D/g, ""),
    company: data.company,
    source: data.source,
    owner: data.owner || existing?.owner || currentOwner(),
    stage: data.stage,
    level: data.level,
    note: data.note,
    amount: Number(data.amount || 0),
    city: data.city,
    nextFollowAt: data.nextFollowAt || null,
    tags: data.tags ? data.tags.split(",").map(item => item.trim()).filter(Boolean) : (existing?.tags || ["新客户"]),
    gender: data.gender, birthday: data.birthday, age: data.age, height: data.height,
    maritalStatus: data.maritalStatus, education: data.education, monthlyIncome: data.monthlyIncome,
    annualIncome: data.annualIncome, occupation: data.occupation, housing: data.housing, car: data.car,
    nativePlace: data.nativePlace, workLocation: data.workLocation, wechat: data.wechat, idCard: data.idCard, remark: data.remark,
    certificationStatus: data.certificationStatus, familyStatus: data.familyStatus, childrenStatus: data.childrenStatus,
    vehicleHousing: data.vehicleHousing, matchAgeRange: data.matchAgeRange, matchMaritalStatus: data.matchMaritalStatus,
    matchHeightRange: data.matchHeightRange, matchEducation: data.matchEducation, matchMonthlyIncome: data.matchMonthlyIncome,
    matchMostImportant: data.matchMostImportant, matchPersonality: data.matchPersonality, matchChildren: data.matchChildren,
    matchDealbreakers: data.matchDealbreakers, collaborator: existing?.collaborator || ""
  };
  try {
    requireBackend();
    const duplicateBefore = !existing ? customers.concat(state.poolCustomers || []).find(customer => String(customer.phone || "").replace(/\D/g, "") === payload.phone) : null;
    const result = await apiRequest(existing ? `/customers/${encodeURIComponent(existing.id)}` : "/customers", { method: existing ? "PUT" : "POST", body: JSON.stringify(payload) });
    const saved = normalizeCustomer(result);
    const index = customers.findIndex(customer => customer.id === saved.id);
    if (index >= 0) customers[index] = saved; else customers.unshift(saved);
    state.dashboard = { ...state.dashboard, newCustomers: customers.length, totalCustomers: customers.length };
    closeModal();
    const duplicateRegistration = duplicateBefore && saved.id === duplicateBefore.id && saved.registrationCount > Number(duplicateBefore.registrationCount || 1);
    toast(duplicateRegistration ? `客户 ${saved.name} 已完成第 ${saved.registrationCount} 次注册` : `客户 ${data.name } 已${existing ? "更新" : "创建"}并写入 MySQL`);
    render();
  } catch (error) {
    toast(`客户保存失败：${error.message}`);
  }
});
document.querySelector("#businessForm").addEventListener("submit", submitBusinessForm);

function globalSearchCustomer(query) {
  const value = String(query || "").trim().toLowerCase();
  const digits = value.replace(/\D/g, "");
  if (!value) return null;
  const pool = [...customers, ...state.poolCustomers];
  return pool.find(item => {
    const id = String(item.id || item.customerNo || "").trim().toLowerCase();
    const name = String(item.name || item.displayName || item.username || item.account || "").trim().toLowerCase();
    const phone = String(item.phone || "").replace(/\D/g, "");
    const company = String(item.company || "").trim().toLowerCase();
    return id === value || name === value || company === value || (digits && phone === digits);
  }) || null;
}

document.querySelector("#globalSearch").addEventListener("keydown", event => {
  if (event.key !== "Enter") return;
  const query = event.target.value.trim();
  if (!query) return;
  const customer = globalSearchCustomer(query);
  if (customer) {
    event.target.value = "";
    openCustomer(customer.id);
  } else {
    toast("该用户不存在或您无权限查看", "error");
  }
});
document.addEventListener("keydown", event => {
  if ((event.metaKey || event.ctrlKey) && event.key.toLowerCase() === "k") { event.preventDefault(); document.querySelector("#globalSearch").focus(); }
  if (event.key === "Escape") { closeDrawer(); closeModal(); closeBusinessModal(); document.querySelectorAll(".popover").forEach(item => item.hidden = true); }
});

const notificationPopover = document.querySelector("#notificationPopover");
document.querySelector("#notificationButton").addEventListener("click", event => { event.stopPropagation(); notificationPopover.hidden = !notificationPopover.hidden; document.querySelector("#userPopover").hidden = true; });
document.querySelector("#userMenu").addEventListener("click", event => { event.stopPropagation(); const popover = document.querySelector("#userPopover"); popover.hidden = !popover.hidden; notificationPopover.hidden = true; });
document.querySelector("#accountSwitchButton").addEventListener("click", () => { document.querySelector("#userPopover").hidden = true; openAccountSwitcher(); });
document.querySelector("#logoutButton").addEventListener("click", () => { document.querySelector("#userPopover").hidden = true; logout(); });
document.querySelector("#profileButton").addEventListener("click", () => { document.querySelector("#userPopover").hidden = true; openBusinessModal("profile"); });
document.querySelector("#settingsButton").addEventListener("click", () => { document.querySelector("#userPopover").hidden = true; openBusinessModal("settings"); });
document.querySelector("#userHelpButton").addEventListener("click", () => { document.querySelector("#userPopover").hidden = true; openHelpModal(); });
document.querySelector("#closeAccountModal").addEventListener("click", closeAccountSwitcher);
document.querySelector("#cancelAccountModal").addEventListener("click", closeAccountSwitcher);
document.querySelector("#accountBackdrop").addEventListener("click", event => { if (event.target.id === "accountBackdrop") closeAccountSwitcher(); });
document.querySelector("#accountSwitchForm").addEventListener("submit", submitAccountSwitch);
notificationPopover.addEventListener("click", async event => {
  const notificationButton = event.target.closest("[data-popover-notification]");
  if (notificationButton) {
    const row = systemNotificationRows().find(item => item.id === notificationButton.dataset.popoverNotification);
    notificationPopover.hidden = true;
    if (row) await openSystemNotification(row);
    return;
  }
  if (event.target.closest("#readNotifications")) {
    notificationPopover.hidden = true;
    await markAllSystemNotificationsRead();
    return;
  }
  if (event.target.closest("[data-open-notification-center]")) {
    notificationPopover.hidden = true;
    state.messageSection = "短信记录";
    navigate("messages");
  }
});
document.addEventListener("click", event => {
  if (collaborationOwnerPopover && !event.target.closest(".collaboration-owner-popover") && !event.target.closest("[data-collaboration-owner-search]")) closeCollaborationOwnerPopover();
  if (!event.target.closest(".popover") && !event.target.closest("#notificationButton") && !event.target.closest("#userMenu")) document.querySelectorAll(".popover").forEach(item => item.hidden = true);
  if (!event.target.closest(".customer-more-actions")) document.querySelectorAll(".customer-more-menu.open").forEach(item => item.classList.remove("open"));
  let filterStateChanged = false;
  if (!event.target.closest(".date-range-picker-wrap") && !event.target.closest(".advanced-date-range-control")) {
    if (state.customerDateRangePickerOpen || state.customerAdvancedDatePickerOpen) filterStateChanged = true;
    state.customerDateRangePickerOpen = false;
    state.customerAdvancedDatePickerOpen = false;
    state.customerAdvancedDatePickerField = "";
  }
  if (!event.target.closest(".status-filter") && !event.target.closest("#stageFilter")) document.querySelectorAll(".status-filter.open").forEach(item => item.classList.remove("open"));
  if (!event.target.closest(".owner-cascade-filter") && (state.customerOwnerCascadeOpen || state.customerAdvancedOwnerCascadeOpen || state.customerAdvancedCollaboratorCascadeOpen)) {
    state.customerOwnerCascadeOpen = false;
    state.customerAdvancedOwnerCascadeOpen = false;
    state.customerAdvancedCollaboratorCascadeOpen = false;
    filterStateChanged = true;
  }
  if (filterStateChanged) render();
});
document.querySelectorAll("[data-help]").forEach(button => button.addEventListener("click", () => openHelpModal(button.dataset.help === "学习中心" ? "learning" : "help")));
window.addEventListener("hashchange", () => { const next = location.hash.replace("#", ""); if (next && next !== state.view) { state.view = next; render(); } });

async function initialize() {
  if (!state.auth.token) {
    showLogin();
    return;
  }
  try {
    state.auth.user = await apiRequest("/auth/me");
    await hydrateFromApi();
    showApp();
  } catch (_) {
    clearAuth();
    showLogin("无法验证登录状态，请重新登录");
  }
}

initialize();



