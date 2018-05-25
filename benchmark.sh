#!/bin/sh
### BEGIN INIT INFO
# Provides:          benchmark
# Required-Start:    $local_fs $network $named $time $syslog
# Required-Stop:     $local_fs $network $named $time $syslog
# Default-Start:     2 3 4 5
# Default-Stop:      0 1 6
# Description:       Test benchmarks
### END INIT INFO

# Source function library.
if [ -f /etc/init.d/functions ]; then
    . /etc/init.d/functions
elif [ -f /etc/rc.d/init.d/functions ]; then
    . /etc/rc.d/init.d/functions
else
    echo -e "\aDaemon: unable to locate functions lib. Cannot continue."
    exit -1
fi

SCRIPT_VERSION='1.3'

# JAVA_HOME="/opt/jdk1.8.0_65"
RUNAS=`id -u -n`
NAME='benchmark'

PIDFILE=/var/run/$NAME.pid
LOGFILE=/var/log/$NAME.log

HOME_DIR="$( cd "$( dirname "$0" )" && pwd )"
JAR=`find "$HOME_DIR"/lib -iname "$NAME"-*.jar | sort -V | tail -1`

JAVA_OPTS="-server -Xms1536M -Xmx5120M -Xss256k -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
ARGUMENTS=""

clean() {
    rm -r $HOME_DIR/logs/*
}

for param in "$@"; do
    if [[ $param == --* ]] ;then
        IFS='=' read -ra argument <<< "$param"
        case "${argument[0]}" in
            '--debug')
                JAVA_OPTS="$JAVA_OPTS -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.port=7091 -Dcom.sun.management.jmxremote.local.only=false -Dcom.sun.management.jmxremote.rmi.port=7091 -Djava.rmi.server.hostname=localhost"
                ;;
            '--log-gc')
                JAVA_OPTS="$JAVA_OPTS -XX:+PrintGCDetails -Xloggc:$HOME_DIR/logs/gc.log"
                ;;
            '--clean')
                clean
                ;;
            '--args')
                ARGUMENTS="${argument[1]}"
                ;;
            *)
                echo "Unsupport argument: ${argument[0]}"
                printf "  \e[1m%-20s\e[0m%s\n" "--debug" "Open Java JMX model."
                printf "  \e[1m%-20s\e[0m%s\n" "--log-gc" "Print GC details to gc.log."
                printf "  \e[1m%-20s\e[0m%s\n" "--clean" "Clean the logs."
                printf "  \e[1m%s\e[0m=\e[4m%s\e[0m    %s\n" "--args" "ARGUMENTS" "Program arguments."
        esac
    fi
done
SCRIPT="$JAVA_HOME/bin/java $JAVA_OPTS -DAPP_HOME=$HOME_DIR -jar $JAR $ARGUMENTS"

start() {
    echo $SCRIPT > $LOGFILE

    if [ -f $PIDFILE ] && kill -0 $(cat $PIDFILE) 2>/dev/null; then
        echo -e "$NAME (\033[0;31m`cat $PIDFILE`\033[0m) already running." >&2
        return 1
    fi
    local CMD="$SCRIPT &>> \"$LOGFILE\" & echo \$!"
    su -c "$CMD" $RUNAS > "$PIDFILE"
    echo -ne "Starting $NAME (\033[0;31m`cat $PIDFILE`\033[0m) ..." >&2
    RETVAL=$?
    [ $RETVAL -eq 0 ] && echo_success || echo_failure
    echo
    return $RETVAL
}

stop() {
    if [ ! -f $PIDFILE ] || ! kill -0 $(cat "$PIDFILE") 2>/dev/null; then
        echo "$NAME is not running." >&2
        return 1
    fi
    echo -ne "Stopping $NAME (\033[0;31m`cat $PIDFILE`\033[0m) ..." >&2
    kill -15 $(cat "$PIDFILE") && rm -f "$PIDFILE"
    RETVAL=$?
    [ $RETVAL -eq 0 ] && echo_success || echo_failure
    echo
    return $RETVAL
}

run() {
    echo $SCRIPT
    `$SCRIPT`
}

log() {
    PARAM='-10f'
    if [ -n "$2" ] ; then
        PARAM="$2"
    fi
    tail $PARAM $LOGFILE
}

install() {
    SERVICE_FILE="/etc/init.d/$NAME"
    if [ -f "$SERVICE_FILE" ]; then
        echo "Error: service '$NAME' already exists"
        exit 1
    fi
    cp -r $0 $SERVICE_FILE
    local temp="${HOME_DIR//\//\\/}"
    sed -i "s/^HOME_DIR=.*/HOME_DIR='$temp'/g" $SERVICE_FILE
    temp="${JAVA_HOME//\//\\/}"
    sed -i "s/^\(#\s*\)\?JAVA_HOME.*/JAVA_HOME='$temp'/g" $SERVICE_FILE
    chmod +x $SERVICE_FILE
    chkconfig --add $NAME
    echo -e "Script \e[1m$NAME\e[0m was installed."
    stop
    service $NAME start
}

uninstall() {
    echo -n "Are you really sure you want to uninstall this service? That cannot be undone. [yes|No] "
    local SURE
    read SURE
    if [ "$SURE" = "yes" ]; then
        stop
        rm -f "$PIDFILE"
        echo "Notice: log file is not be removed: '$LOGFILE'" >&2
        chkconfig --del $NAME
        rm -fv "$0"
    fi
}

status() {
    if [ -f $PIDFILE ] && kill -0 $(cat $PIDFILE) 2>/dev/null; then
        echo -e "$NAME (\033[0;31m`cat $PIDFILE`\033[0m) is running."
    else
        echo "$NAME is not running."
    fi
}

pack() {
    tar zvcf $HOME_DIR.tar.gz $HOME_DIR --exclude=logs/*
}

manifest() {
    unzip -q -c $JAR META-INF/MANIFEST.MF
}

info() {
    echo "System Information:"
    echo
    echo `head -n 1 /etc/issue`
    echo `uname -sor`
    echo
    echo `$JAVA_HOME/bin/java -version`
    echo "JAVA_HOME:$JAVA_HOME"
    echo "Jar      :$JAR"
    echo "Hash     :`md5sum $JAR | awk '{print($1)}'`"
}

case "$1" in
    start)
        start
        ;;
    stop)
        stop
        ;;
    restart)
        stop
        start
        ;;
    run)
        run
        ;;
    log)
        log $@
        ;;
    clean)
        clean
        ;;
    install)
        install
        ;;
    uninstall)
        uninstall
        ;;
    status)
        status
        ;;
    info)
        info
        ;;
    pack)
        pack
        ;;
    *)
        echo "Usage: $0 [OPTION]... [--ARGS]..."
        printf "  \e[1m%-20s\e[0m%s\n" "start" "start application as deamon."
        printf "  \e[1m%-20s\e[0m%s\n" "stop" "stop application."
        printf "  \e[1m%-20s\e[0m%s\n" "restart" "restart application."
        printf "  \e[1m%-20s\e[0m%s\n" "run" "run application in the command line."
        printf "  \e[1m%-20s\e[0m%s\n" "log" "view application SYSOUT."
        printf "  \e[1m%-20s\e[0m%s\n" "clean" "clean jumk file in the $HOME_DIR. including all file in $HOME_DIR/logs/"
        printf "  \e[1m%-20s\e[0m%s\n" "install" "install this script as a system service."
        printf "  \e[1m%-20s\e[0m%s\n" "uninstall" "uninstall this script from system servcie."
        printf "  \e[1m%-20s\e[0m%s\n" "status" "display application running status."
        printf "  \e[1m%-20s\e[0m%s\n" "info" "display information of application."
        printf "  \e[1m%-20s\e[0m%s\n" "pack" "package all file in $HOME_DIR to a gzip archive. without file in the logs directory."
esac
