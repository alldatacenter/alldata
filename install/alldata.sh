#!/bin/sh
# =============================================================================
#  alldata.sh —— AllData 平台各服务统一启动 / 停止 / 状态控制脚本
# =============================================================================
#
#  用法:
#      sh alldata.sh <command> [target]
#
#  command:
#      start      启动服务
#      stop       停止服务
#      restart    重启服务 (等价于 stop + start)
#      status     查看服务运行状态
#      list       列出全部服务 (分组 / 端口 / 运行状态)
#      logs       实时跟踪某个服务的日志 (tail -f)
#      help       显示本帮助
#
#  target:  省略时默认为 all
#      <service>   单个服务, 例如 eureka / gateway / data-server
#      <group>     整组服务, 例如 16gmaster / 16gslave / 16gdata
#                  (可简写为 master / slave / data)
#      all         全部服务
#
#  示例:
#      sh alldata.sh start eureka            # 启动 eureka
#      sh alldata.sh stop eureka             # 停止 eureka
#      sh alldata.sh restart gateway         # 重启 gateway
#      sh alldata.sh status 16gmaster        # 查看 master 组运行状态
#      sh alldata.sh start all               # 按依赖顺序启动全部服务
#      sh alldata.sh list                    # 列出全部服务
#      sh alldata.sh logs data-server        # 跟踪 data-server 日志
#
#  可用环境变量:
#      JAVA_HOME      指定 JDK, 未设置时使用 PATH 中的 java
#      CORE_WAIT      启动 config / eureka 之后等待的秒数(默认 10)
#      STOP_TIMEOUT   优雅停止超时秒数, 超时后使用 kill -9(默认 20)
#
# =============================================================================

# 加载系统环境, 保证 java 已在 PATH 中 (沿用原有脚本的做法)
[ -f /etc/profile ] && . /etc/profile 2>/dev/null || true

# 脚本所在目录: 发布包解压后的根目录, 或源码仓库中的 install/
BASE_DIR=$(cd "$(dirname "$0")" && pwd)

CORE_WAIT=${CORE_WAIT:-10}
STOP_TIMEOUT=${STOP_TIMEOUT:-20}

# java 可执行文件: 优先使用 JAVA_HOME
JAVA_BIN=java
if [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/java" ]; then
	JAVA_BIN="$JAVA_HOME/bin/java"
fi

# 各服务统一使用的 JVM 参数 (与原有各服务脚本保持一致)
GC_OPTS="-XX:PermSize=128M -XX:MaxPermSize=256M -XX:+UseG1GC -XX:MaxGCPauseMillis=20 -XX:InitiatingHeapOccupancyPercent=35 -XX:+ExplicitGCInvokesConcurrent -XX:MaxInlineLevel=15"

# -----------------------------------------------------------------------------
#  服务注册表
#    字段: <相对目录>|<jar 文件>|<端口>|<profile 或 ->|<Xms>|<Xmx>|<日志文件>
#    目录与日志路径均相对于 BASE_DIR
#
#    说明: 这里把原有 19 个服务脚本的参数集中到一处, 作为唯一事实来源。
#          新增服务只需在此登记, 无需再写单独的启动脚本。
# -----------------------------------------------------------------------------
svc_meta() {
	case "$1" in
	# ----------------------------- 16gmaster -----------------------------
	config)
		echo "16gmaster/config|config.jar|8611|-|1024m|2048m|16gmaster/config.log" ;;
	system)
		echo "16gmaster/data-system-service|data-system-service.jar|8810|dev|128m|2048m|16gmaster/data-system-service/data-system-service.log" ;;
	dts)
		echo "16gmaster/service-data-dts|service-data-dts.jar|9536|dev|128m|2048m|16gmaster/service-data-dts/service-data-dts.log" ;;
	server)
		echo "16gmaster/data-server|data-server.jar|8000|dev|128m|2048m|16gmaster/data-server/data-server.log" ;;
	market)
		echo "16gmaster/data-market-service|data-market-service.jar|8822|dev|128m|2048m|16gmaster/data-market-service/data-market-service.log" ;;
	metadata)
		echo "16gmaster/data-metadata-service|data-metadata-service.jar|8820|dev|128m|2048m|16gmaster/data-metadata-service/data-metadata-service.log" ;;
	market-integration)
		echo "16gmaster/data-market-service-integration|data-market-service-integration.jar|8824|dev|128m|2048m|16gmaster/data-market-service-integration/data-market-service-integration.log" ;;
	compare)
		echo "16gmaster/data-compare-service|data-compare-service.jar|8096|dev|128m|2048m|16gmaster/data-compare-service/data-compare-service.log" ;;
	# ----------------------------- 16gslave ------------------------------
	eureka)
		echo "16gslave/eureka|eureka.jar|8610|-|1024m|2048m|16gslave/eureka.log" ;;
	market-mapping)
		echo "16gslave/data-market-service-mapping|data-market-service-mapping.jar|8823|-|128m|2048m|16gslave/data-market-service-mapping.log" ;;
	masterdata)
		echo "16gslave/data-masterdata-service|data-masterdata-service.jar|8828|-|128m|2048m|16gslave/data-masterdata-service.log" ;;
	metadata-console)
		echo "16gslave/data-metadata-service-console|data-metadata-service-console.jar|8821|-|128m|2048m|16gslave/data-metadata-service-console.log" ;;
	quality)
		echo "16gslave/data-quality-service|data-quality-service.jar|8826|-|128m|2048m|16gslave/data-quality-service.log" ;;
	# ----------------------------- 16gdata -------------------------------
	gateway)
		echo "16gdata/gateway|gateway.jar|9538|-|1024m|2048m|16gdata/gateway.log" ;;
	standard)
		echo "16gdata/data-standard-service|data-standard-service.jar|8825|-|128m|2048m|16gdata/data-standard-service.log" ;;
	visual)
		echo "16gdata/data-visual-service|data-visual-service.jar|8827|-|128m|2048m|16gdata/data-visual-service.log" ;;
	quartz)
		echo "16gdata/quartz-service|quartz-service.jar|8813|-|128m|2048m|16gdata/quartz-service.log" ;;
	*)
		return 1 ;;
	esac
}

# 从注册表中取某个字段 (1..7)
svc_field() {
	svc_meta "$1" | cut -d'|' -f"$2"
}

svc_dir()  { svc_field "$1" 1; }
svc_jar()  { svc_field "$1" 2; }
svc_port() { svc_field "$1" 3; }
svc_profile() { svc_field "$1" 4; }
svc_xms()  { svc_field "$1" 5; }
svc_xmx()  { svc_field "$1" 6; }
svc_log()  { svc_field "$1" 7; }

# 服务所属分组 (路径第一段)
svc_group() { svc_dir "$1" | cut -d'/' -f1; }

is_service() {
	case " $ALL_SERVICES " in
		*" $1 "*) return 0 ;;
		*) return 1 ;;
	esac
}

# -----------------------------------------------------------------------------
#  服务清单与启动顺序
#    启动顺序按依赖排列: 配置中心 config → 注册中心 eureka → 网关 gateway → 其余
# -----------------------------------------------------------------------------
MASTER_SERVICES="config system dts server market metadata market-integration compare"
SLAVE_SERVICES="eureka market-mapping masterdata metadata-console quality"
DATA_SERVICES="gateway standard visual quartz"

ALL_SERVICES="config eureka gateway system dts server market metadata market-integration compare market-mapping masterdata metadata-console quality standard visual quartz"

CORE_SERVICES="config eureka"

# -----------------------------------------------------------------------------
#  输出辅助
# -----------------------------------------------------------------------------
info()  { echo "[INFO ] $*"; }
warn()  { echo "[WARN ] $*"; }
error() { echo "[ERROR] $*" >&2; }

# 把多行 PID 合并成一行展示 (去掉多余空白)
join_pids() {
	echo "$1" | tr '\n' ' ' | sed 's/[[:space:]]*$//'
}

# -----------------------------------------------------------------------------
#  进程查找 / 状态判断
#    以 PID 文件为主(本脚本启动的服务), 以 ps 为兜底(兼容由原有脚本启动的服务)。
#    只用 ps 的话, 在受限容器里 ps 不可用时 start/stop/status 会全部失灵。
# -----------------------------------------------------------------------------
pidfile() {
	echo "$BASE_DIR/$(svc_dir "$1")/$1.pid"
}

# 判断 pid 是否为本服务的存活进程: 取得到命令行时做二次校验, 避免 PID 复用误判
pid_alive() {
	_pid=$1
	_jar=$2
	kill -0 "$_pid" 2>/dev/null || return 1
	_cmd=$(ps -p "$_pid" -o command= 2>/dev/null)
	if [ -n "$_cmd" ]; then
		case "$_cmd" in
			*"$_jar"*) return 0 ;;
			*) return 1 ;;
		esac
	fi
	# ps 不可用, 无法校验, 以 PID 文件为准
	return 0
}

find_pids() {
	_svc=$1
	_jar=$(svc_jar "$_svc")
	_pf=$(pidfile "$_svc")

	if [ -f "$_pf" ]; then
		_p=$(tr -d ' \t\r\n' < "$_pf" 2>/dev/null)
		case "$_p" in
			''|*[!0-9]*) ;;   # 内容非法, 当作没有
			*)
				if pid_alive "$_p" "$_jar"; then
					echo "$_p"
					return 0
				fi ;;
		esac
	fi

	# 兜底: 通过 ps 查找(例如服务是由原有 xxx.sh 脚本启动的, 没有 PID 文件)
	ps -ef 2>/dev/null | grep java | grep -F "$_jar" | grep -v grep | awk '{print $2}'
}

is_running() {
	[ -n "$(find_pids "$1")" ]
}

# -----------------------------------------------------------------------------
#  启动单个服务
# -----------------------------------------------------------------------------
start_one() {
	_svc=$1
	_dir="$BASE_DIR/$(svc_dir "$_svc")"
	_jar=$(svc_jar "$_svc")
	_port=$(svc_port "$_svc")
	_log="$BASE_DIR/$(svc_log "$_svc")"
	_profile=$(svc_profile "$_svc")

	if [ ! -d "$_dir" ]; then
		warn "[$_svc] 目录不存在, 已跳过: $(svc_dir "$_svc")"
		return 1
	fi
	if [ ! -f "$_dir/$_jar" ]; then
		warn "[$_svc] 未找到 jar, 已跳过: $(svc_dir "$_svc")/$_jar"
		return 1
	fi

	_pids=$(find_pids "$_svc")
	if [ -n "$_pids" ]; then
		info "[$_svc] 已在运行 (PID: $(join_pids "$_pids")), 跳过启动"
		return 0
	fi

	if [ "$_profile" = "-" ]; then
		_pargs=""
	else
		_pargs="--spring.profiles.active=$_profile"
	fi

	# 进入服务目录启动, 使相对路径与原有脚本一致; 启动后立即切回
	cd "$_dir" || return 1
	nohup "$JAVA_BIN" -jar \
		-Xms"$(svc_xms "$_svc")" -Xmx"$(svc_xmx "$_svc")" $GC_OPTS \
		"./$_jar" --server.port="$_port" $_pargs \
		> "$_log" 2>&1 &
	_pid=$!
	cd "$BASE_DIR" || return 1

	# 等 1 秒确认进程没有立刻退出
	sleep 1
	if kill -0 "$_pid" 2>/dev/null; then
		echo "$_pid" > "$(pidfile "$_svc")"
		info "[$_svc] 启动成功 (PID: $_pid, 端口: $_port, 日志: $(svc_log "$_svc"))"
		return 0
	else
		rm -f "$(pidfile "$_svc")"
		error "[$_svc] 启动失败, 请查看日志: $(svc_log "$_svc")"
		return 1
	fi
}

# -----------------------------------------------------------------------------
#  停止单个服务
#    先 SIGTERM 优雅退出, 超时后 SIGKILL
# -----------------------------------------------------------------------------
stop_one() {
	_svc=$1
	_pf=$(pidfile "$_svc")

	_pids=$(find_pids "$_svc")
	if [ -z "$_pids" ]; then
		rm -f "$_pf"
		info "[$_svc] 未在运行, 无需停止"
		return 0
	fi

	info "[$_svc] 正在停止 (PID: $(join_pids "$_pids"))"
	# shellcheck disable=SC2086
	kill $_pids 2>/dev/null

	_n=0
	while [ "$_n" -lt "$STOP_TIMEOUT" ]; do
		[ -z "$(find_pids "$_svc")" ] && break
		sleep 1
		_n=$((_n + 1))
	done

	_left=$(find_pids "$_svc")
	if [ -n "$_left" ]; then
		warn "[$_svc] 优雅退出超时(${STOP_TIMEOUT}s), 强制 kill -9"
		# shellcheck disable=SC2086
		kill -9 $_left 2>/dev/null
		sleep 1
	fi

	if [ -z "$(find_pids "$_svc")" ]; then
		rm -f "$_pf"
		info "[$_svc] 已停止"
		return 0
	else
		error "[$_svc] 停止失败, 仍有进程存活"
		return 1
	fi
}

# -----------------------------------------------------------------------------
#  状态表格
#    注意: printf 的 %-Ns 按"字节"补齐, 而中文占 2 个显示列宽(3 字节),
#          所以表头需要手工补足空格才能和数据行对齐。
# -----------------------------------------------------------------------------
print_table_header() {
	printf '  %s %s %s %s\n' \
		"$(printf '服务%30s' '')" \
		"$(printf '分组%6s' '')" \
		"$(printf '端口%3s' '')" \
		"状态"
	printf '  %s\n' "--------------------------------------------------------------------------"
}

# -----------------------------------------------------------------------------
#  查看单个服务状态 (简洁一行)
# -----------------------------------------------------------------------------
show_one() {
	_svc=$1
	_pids=$(find_pids "$_svc")
	if [ -n "$_pids" ]; then
		printf '  %-34s %-10s %-7s %s\n' \
			"$_svc" "$(svc_group "$_svc")" "$(svc_port "$_svc")" \
			"运行中 (PID: $(join_pids "$_pids"))"
	else
		printf '  %-34s %-10s %-7s %s\n' \
			"$_svc" "$(svc_group "$_svc")" "$(svc_port "$_svc")" "未运行"
	fi
}

# -----------------------------------------------------------------------------
#  目标解析: 把 target 展开成服务列表
# -----------------------------------------------------------------------------
resolve_targets() {
	case "$1" in
	all|ALL|"")
		echo "$ALL_SERVICES" ;;
	16gmaster|master)
		echo "$MASTER_SERVICES" ;;
	16gslave|slave)
		echo "$SLAVE_SERVICES" ;;
	16gdata|data)
		echo "$DATA_SERVICES" ;;
	*)
		if is_service "$1"; then
			echo "$1"
		else
			error "未知的服务或分组: $1"
			echo "       可用服务: $ALL_SERVICES" >&2
			echo "       可用分组: 16gmaster | 16gslave | 16gdata | all" >&2
			return 1
		fi ;;
	esac
	return 0
}

# -----------------------------------------------------------------------------
#  各命令实现
# -----------------------------------------------------------------------------
cmd_start() {
	_targets=$1
	_tmp=$(resolve_targets "$_targets") || return 1
	[ -n "$_tmp" ] || return 1

	_ok=0
	_fail=0
	_count=$(echo "$_tmp" | wc -w | tr -d ' ')
	info "准备启动: $_tmp"
	for _svc in $_tmp; do
		start_one "$_svc" && _ok=$((_ok + 1)) || _fail=$((_fail + 1))
		# 启动多个服务时, 在核心服务(config / eureka)之后就绪等待
		if [ "$_count" -gt 1 ]; then
			case " $CORE_SERVICES " in
				*" $_svc "*)
					[ "$CORE_WAIT" -gt 0 ] && sleep "$CORE_WAIT" ;;
			esac
		fi
	done

	echo
	info "启动完成: 成功 $_ok 个, 失败 $_fail 个 (共 $_count 个)"
	[ "$_fail" -eq 0 ]
}

cmd_stop() {
	_targets=$1
	_tmp=$(resolve_targets "$_targets") || return 1
	[ -n "$_tmp" ] || return 1

	_ok=0
	_fail=0
	# 停止时按启动顺序的反序, 避免依赖服务先挂
	_rev=""
	for _svc in $_tmp; do _rev="$_svc $_rev"; done

	info "准备停止: $_tmp"
	for _svc in $_rev; do
		stop_one "$_svc" && _ok=$((_ok + 1)) || _fail=$((_fail + 1))
	done

	echo
	info "停止完成: 成功 $_ok 个, 失败 $_fail 个"
	[ "$_fail" -eq 0 ]
}

cmd_restart() {
	_targets=$1
	_tmp=$(resolve_targets "$_targets") || return 1
	[ -n "$_tmp" ] || return 1

	info "准备重启: $_tmp"
	for _svc in $_tmp; do
		stop_one "$_svc"
	done
	sleep 2
	cmd_start "$_targets"
}

cmd_status() {
	_targets=$1
	_tmp=$(resolve_targets "$_targets") || return 1
	[ -n "$_tmp" ] || return 1

	print_table_header
	for _svc in $_tmp; do
		show_one "$_svc"
	done
}

cmd_list() {
	print_table_header
	for _svc in $ALL_SERVICES; do
		show_one "$_svc"
	done
}

cmd_logs() {
	_svc=$1
	if [ -z "$_svc" ]; then
		error "logs 命令需要指定服务名, 例如: sh alldata.sh logs eureka"
		return 1
	fi
	if ! is_service "$_svc"; then
		error "未知的服务: $_svc"
		return 1
	fi

	_log="$BASE_DIR/$(svc_log "$_svc")"
	if [ ! -f "$_log" ]; then
		error "日志文件不存在: $(svc_log "$_svc")"
		return 1
	fi
	info "跟踪日志: $(svc_log "$_svc")  (Ctrl+C 退出)"
	tail -n 100 -f "$_log"
}

usage() {
	# 直接复用文件头部的注释作为帮助文本, 避免帮助与实现两处维护
	sed -n '2,38p' "$0" | sed 's/^# \{0,1\}//'
}

# -----------------------------------------------------------------------------
#  主流程
# -----------------------------------------------------------------------------
COMMAND=$1
TARGET=$2
[ -z "$TARGET" ] && TARGET=all

case "$COMMAND" in
start)   cmd_start   "$TARGET" ;;
stop)    cmd_stop    "$TARGET" ;;
restart) cmd_restart "$TARGET" ;;
status)  cmd_status  "$TARGET" ;;
list)    cmd_list ;;
logs)    cmd_logs    "$2" ;;
help|-h|--help|"")
	usage ;;
*)
	error "未知命令: $COMMAND"
	echo
	usage
	exit 1 ;;
esac
