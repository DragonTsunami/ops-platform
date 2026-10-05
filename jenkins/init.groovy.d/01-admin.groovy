// jenkins/init.groovy.d/01-admin.groovy — Jenkins 初始化即代码
// why：免 8 步手工向导——compose 拉起即得可用 admin（密码走 .env 注入，脚本从环境变量读，不落 git）
import jenkins.model.*
import hudson.security.*

def env = System.getenv()
def adminPass = env['JENKINS_ADMIN_PASSWORD']

if (adminPass) {
    def hudsonRealm = new HudsonPrivateSecurityRealm(false)   // false=不允许用户自助注册（安全基线）
    hudsonRealm.createAccount('admin', adminPass)
    Jenkins.instance.setSecurityRealm(hudsonRealm)

    def strategy = new FullControlOnceLoggedInAuthorizationStrategy()
    strategy.setAllowAnonymousRead(false)                      // why：匿名连读都关（面板栈安全基线统一）
    Jenkins.instance.setAuthorizationStrategy(strategy)

    Jenkins.instance.save()
    println('>> init.groovy: admin 账号已建 + 登录墙已立（配置即代码，跳过手工向导）')
} else {
    println('>> init.groovy: JENKINS_ADMIN_PASSWORD 未注入，跳过（保留向导模式）')
}

// why 关更新检查：离线/内网环境避免每次启动卡 update center 探测
Jenkins.instance.setNumExecutors(2)
println('>> init.groovy: executors=2')
