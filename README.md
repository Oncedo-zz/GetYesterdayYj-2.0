# GetYesterdayYj 2.0

昨日销售业绩自动统计工具。自动登录 E3ERP 系统，抓取终端业绩统计，写入 Excel 并同步 WPS 云文档。

## 相比 1.0 的改进

1. **配置驱动**：所有页面选择器、URL、输出路径、等待时长集中在 `src/main/resources/config/sites.yml`。服务端页面改版时，只改配置即可，无需改代码、无需重新编译。
2. **多候选定位**：每个元素可配多个候选选择器，主选择器失效自动降级尝试备选。
3. **失败即报**：登录失败、定位失败、数据为空都会明确抛出异常并指出卡在哪一步，不再"登录后卡住空转"。
4. **站点通用化**：抓取逻辑合并为一个引擎，新增站点只需在 yml 里加一段 `sites.<key>`。
5. **密钥外置**：邮箱、WPS Token、账号从源码移到配置 + 环境变量。
6. **自检模式**：`--check` 不启动浏览器即可校验配置是否就绪。

## 快速开始

### 1. 环境要求

- JDK 17（系统 PATH 里的 java 若为 1.8 会导致启动失败，请用 17）
- Chrome + 对应版本 ChromeDriver
- E3ERP 登录插件（如 `C:\Plugs\e3erp`）

### 2. 配置凭据

设置环境变量（格式与 1.0 一致）：

```
JTNRB=jtnrb账号,jtnrb密码,yhgz账号,yhgz密码
```

可选覆盖（不设置则用内置默认值）：

```
JTNRB_URL=http://e3erp.jtnrb2017.cn/
YHGZ_URL=http://e3erp.yghz2022.cn/
WPS_TOKEN=xxx
MAIL_SENDER=xxx@qq.com
MAIL_PASSWORD=授权码
MAIL_RECIPIENT=xxx@qq.com
```

### 3. 自检

```bash
java -jar GetYesterdayYj-2.0.jar --check
```

打印配置来源、站点、选择器数量、凭据是否就绪（密钥脱敏），不启动浏览器、不发任何网络请求。

### 4. 运行

```bash
java -jar GetYesterdayYj-2.0.jar              # 默认昨日
java -jar GetYesterdayYj-2.0.jar 0906         # 单个日期
java -jar GetYesterdayYj-2.0.jar 0906 0908    # 日期区间
```

Windows 用户可直接双击 `获取指定日期区间的业绩_2.0.bat`。

## 服务端改版怎么办

在 jar **同目录**放一个 `sites.yml`（可从 `src/main/resources/config/sites.yml` 复制），只写要改的部分，例如：

```yaml
sites:
  jtnrb:
    login:
      username:
        - 'css=input[placeholder="请输入用户名"]'   # 新页面改了 placeholder
```

程序会自动用外部文件覆盖内置配置（深度合并），改完保存即可，无需重新打包。可在任意元素下追加多个候选选择器，按顺序尝试。

## 配置结构说明

| 节点 | 说明 |
|---|---|
| `defaults.wait.*` | 各类等待时长（毫秒/秒） |
| `defaults.driver.*` | ChromeDriver 路径、启动参数、插件目录 |
| `sites.<key>.url` | 站点登录地址 |
| `sites.<key>.credentials` | 账号取自哪个环境变量、在其中的下标 |
| `sites.<key>.login.*` | 登录页账号/密码/按钮/成功标志的选择器候选 |
| `sites.<key>.navigation` | 菜单导航步骤选择器 |
| `sites.<key>.report.*` | 日期输入、查询按钮、表格、表头选择器；`dateColumnOffset` 为数据行"销售日期"列偏移 |
| `sites.<key>.output.*` | Excel 输出目录、文件名模板、sheet 名 |
| `integrations.wps.*` | WPS 云文档同步地址、Token、进程名 |
| `integrations.mail.*` | 邮件 SMTP 配置 |

选择器语法：`css=` / `xpath=` / `id=` / `class=` / `tag=` / `name=` / `text=` 前缀 + 表达式。

## 安全提示

`sites.yml` 中的邮箱授权码、WPS Token 为示例默认值（取自原 1.0 源码）。**上传或公开仓库前请改为占位符，并通过环境变量 `MAIL_PASSWORD`、`WPS_TOKEN` 提供真实值。** 运行目录下的 `sites.yml` 已加入 `.gitignore`，不会入库。

## 构建

```bash
mvn clean package
```

产物：`target/GetYesterdayYj-2.0.jar`
