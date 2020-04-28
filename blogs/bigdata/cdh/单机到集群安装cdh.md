# 1 单机到集群安装cdh5.16
cdh5.16单机到集群安装，通过VM虚拟机安装。

## 1.1 文档规范
* 代码块和vi编辑块用灰色区域标记
* $后跟的linux命令

# 2 安装准备

## 2.1 节点分配
操作系统：CentOS7.5  
节点分配如下表  

| 节点 | 组件 |
| :-----| ----: |
| cdh-master | server |
| cdh01  | agent |
| cdh02  | agent |
| cdh03  | agent |
| cdh04  | agent |
| cdh05  | agent |

## 2.2 网络配置

### 2.2.1 VM配置固定IP
非VM安装可跳过本步骤。
本教程使用VM的“NAT方式配置固定IP”，安装步骤参考：[Vmware虚拟机网络配置(固定IP)](https://www.jianshu.com/p/6fdbba039d79)  
虚拟机网络配置参数设置如下
~~~
# 固定IP网络参数配置
$ vi /etc/sysconfig/network-scripts/ifcfg-ens33
BOOTPROTO=static
NM_CONTROLLED=yes
IPADDR=192.168.37.1
ONBOOT=yes
NETMASK=255.255.255.0
GATEWAY=192.168.37.2
DNS1=114.114.114.119
DNS2=114.114.115.119

# 重启网卡
$ service network restart

# 验证
$ ifconfig
~~~

### 2.2.2 设置主机名和ip
~~~
# 设置主机名
3种方式查看主机名：
$ hostname
$ cat /etc/hostname 
$ hostnamectl status
master设置主机名：
$ hostnamectl set-hostname hpd-master
slave设置主机名参考master


# 编辑/etc/hosts，配置ip
vi /etc/hosts
192.168.37.1  cdh-master
192.168.37.2  cdh01
192.168.37.3  cdh02
192.168.37.4  cdh03
192.168.37.5  cdh04
192.168.37.6  cdh05

~~~

## 2.3 时间同步
集群的时间需要配置为一致的

### 2.3.1 NTP同步时间
配置比较复杂，同步时发现操作未生效，我们机器比较少，因此使用更便捷的手动同步时间方案

### 2.3.2 手动同步时间
~~~
# 注意使用xhell全局命令，可同时设置时间，保证了多台服务器时间是一致的
$ ln -s /usr/share/zoneinfo/Asia/Shanghai /etc/localtime
执行tzselect命令 --> 选择Asia --> 选择China --> 选择Beijing -->选择yes 
$ sed -i '$a\export TZ=Asia/Shanghai' /etc/profile && source /etc/profile && date

$ timedatectl set-ntp no
$ timedatectl set-time "2019-07-24 17:41:00"
~~~

### 2.4 SSH免密登录
步骤如下：
~~~
# 所有节点生成ssh公钥和私钥
$ ssh-keygen -t rsa   #一路回车到完成
# 复制每个节点的公钥和私钥到hdp-master节点
$ ssh-copy-id -i ~/.ssh/id_rsa.pub root@hdp-master
#将复制后的公钥分发给所有节点（包括本机，单机版hdp也需做这步操作)，在hdp-master上操作，依次复制到01-05机器
$ scp ~/.ssh/authorized_keys root@hdp01:~/.ssh/ 
#公钥授权，可能要做，我这里没做，服务是正常的，命令保留参考
$ chmod 700 ~/.ssh
$ chmod 644 ~/.ssh/authorized_keys
#测试
$ ssh root@hdp01
~~~

### 2.5 关闭防火墙和关闭SELINUX
~~~
# 关闭防火墙步骤如下：
# 停止firewall
$ systemctl stop firewalld.service
# 禁止firewall开机启动
$ systemctl disable firewalld.service
# 查看默认防火墙状态（关闭后显示notrunning，开启后显示running
$ firewall-cmd --state
# 关闭selinux，注意关闭后需要重启(这里修改号后，需要重启)。
$ vim /etc/selinux/config 
SELINUX=disabled
查看selinux状态
$ sestatus -v  # SELinux status: disabled 表示已经关闭了
~~~

### 2.6 安装JDK
~~~
我们不使用系统自带的openjdk，安装下载好的jdk1.8。
# 卸载jdk
$ rpm -qa|grep jdk
# 如果有对应的jdk版本，则对其进行卸载，注意替换为实际的openjdk版本
$ rpm -e --nodeps java-1.8.0-openjdk-headless-1.8.0.161-2.b14.el7.x86_64
$ rpm -e --nodeps java-1.8.0-openjdk-1.8.0.161-2.b14.el7.x86_64

# 安装jdk1.8
解压后的java路径为/usr/local/java/jdk1.8.0_201，配置好环境变量即可。
$ vim /etc/profile
export JAVA_HOME=/usr/local/java/jdk1.8.0_201
export CLASSPATH=.:$CLASSPTAH:$JAVA_HOME/lib
export PATH=$PATH:$JAVA_HOME/bin
$ source /etc/profile    #使环境变量生效
$ java -version       #查看JDK是否安装正确
~~~

### 2.7 其他依赖安装

### 2.7.1 所有节点安装
~~~
# 依赖安装
$ yum -y install python-lxml httpd mod_ssl cyrus-sasl-plain  cyrus-sasl-devel  cyrus-sasl-gssapi

# 启动httpd服务并加到开机启动服务中：
$ service httpd start
$ chkconfig httpd on

# 启动httpd报警，解决报警【Starting httpd: httpd: Could not reliably determine the server’s fully qualified domain name, using 192.168.37.1 for ServerName】
$ vim /etc/httpd/conf/httpd.conf
#注释 ServerName www.example.com:80
#添加 ServerName localhost:80

# 重启httpd服务
$ service httpd restart
~~~

# 3 MYSQL安装
主节点安装即可，存储元数据使用

## 3.1 MYSQL安装
~~~
# 下载、解压MYSQL
$ tar -zxvf mysql-5.7.25-linux-glibc2.12-x86_64.tar.gz
$ mkdir /usr/local/mysql
$ cp -rf mysql-5.7.25-linux-glibc2.12-x86_64/* /usr/local/mysql

# 配置MSYQL环境变量
$ vim /etc/profile
MYSQL_HOME=/usr/local/mysql
PATH=$PATH:$MYSQL_HOME/bin
export PATH MYSQL_HOME
$ source /etc/profile

# 创建mysql用户和组
$ groupadd -r mysql && useradd -r -g mysql -s /sbin/nologin -M mysql

# 创建data目录
$ mkdir -p /data/mysql && cd /usr/local/mysql

#初始化mysql5.7数据库
$ ./bin/mysqld --initialize --user=mysql --datadir=/data/mysql
注意保存生成的临时密码 &bsY(o#fe0TS，然后继续执行
$ ./bin/mysql_ssl_rsa_setup --datadir=/data/mysql

# 配置文件修改
$ vi /etc/my.cnf
# These are commonly set, remove the # and set as required.
basedir=/usr/local/mysql //指定程序路径
datadir=/data/mysql //指定数据存放路径
port=3306 //指定端口号
# server_id = .....
socket=/tmp/mysql.sock //指定sock文件

# 复制启动文件并修改相关参数
$ cd /usr/local/mysql
$ cp ./support-files/mysql.server /etc/init.d/mysqld
$ vi /etc/init.d/mysqld
basedir=/usr/local/mysql //指定程序路径
datadir=/data/mysql //指定数据存放路径

# 创建日志目录
$ mkdir /var/log/mariadb 
$ touch /var/log/mariadb/mariadb.log 
$ chown -R mysql:mysql  /var/log/mariadb/

# 安装好后配置
$ chkconfig --add mysqld //加入开机启动
$ /etc/init.d/mysqld start //启动mysql服务
$ ps aux |grep mysqld // 查看mysql进程
$ netstat -ntlp | grep 3306 //查看3306端口监听情况

# 修改密码及授权远程访问
$ mysql -uroot -p'&bsY(o#fe0TS'
# set password = password('root');
# grant all privileges on *.* to 'root'@'%' identified by 'root' with grant option;
# flush privileges;

# 编码配置，防止中文乱码
$ /etc/init.d/mysqld stop
$ vi /etc/my.cnf
[mysqld]
character-set-server=utf8 
[client]
default-character-set=utf8 
[mysql]
default-character-set=utf8
$ /etc/init.d/mysqld start

# 验证编码是否已经配置好了
$ mysql -uroot -p'root'
# show variables like '%char%';
~~~

### 3.2 SQL数据库脚本安装
脚本文件为[cdh_mysql5.7.sql](./files/cdh_mysql5.7.sql)  
执行该脚本,初始化元数据即可。
~~~
$ mysql -uroot -p'root'
# source ./cdh_mysql5.7.sql
~~~ 

# 4 CDH安装

## 4.1 资源清单
| Type | Name | notes |
| :-----| ----: | :----: |
| CDH | CDH-5.16.1-1.cdh5.16.1.p0.3-el7.parcel | [down](http://archive.cloudera.com/cdh5/parcels/5.16.1/) |
| CDH | CDH-5.16.1-1.cdh5.16.1.p0.3-el7.parcel.sha1 |  |
| CDH | cloudera-manager-centos7-cm5.16.1_x86_64.tar.gz | |
| CDH | manifest.json |  |
| kafka | KAFKA-4.0.0-1.4.0.0.p0.1-el7.parcel | [down](http://archive.cloudera.com/kafka/parcels/) |
| kafka | KAFKA-4.0.0-1.4.0.0.p0.1-el7.parcel.sha1 |  |
| kafka | manifest.json | |
| kafka | manifest.json | |
| spark2 | SPARK2-2.3.0.cloudera3-1.cdh5.13.3.p0.458809-el7.parcel | [down](http://archive.cloudera.com/spark2/parcels/2.3.0.cloudera3/) |
| spark2 | SPARK2-2.3.0.cloudera3-1.cdh5.13.3.p0.458809-el7.parcel.sha1 |  |
| spark2 | SPARK2_ON_YARN-2.3.0.cloudera3.jar | |
| spark2 | manifest.json | |
| jar | mysql-connector-java-5.1.46.jar | [down](http://archive.cloudera.com/spark2/csd/) |

## 4.2 CLOUDERA MANAGER SERVER&AGENT安装
使用root用户安装，安装组件有hdfs、hbase、yarn、hive、zookeeper

### 4.2.1 下载、解压、分配资源
所有节点都需处理：
~~~
$ mkdir -p /opt/cloudera-manager
$ tar -zvxf cloudera-manager-centos7-cm5.16.1_x86_64.tar.gz -C /opt/cloudera-manager
~~~

### 4.2.2 配置CM AGENT
所有agent节点都需处理：
~~~
vim /opt/cloudera-manager/cm-5.16.1/etc/cloudera-scm-agent/config.ini
server_host=cdh-master
~~~

### 4.2.3 创建用户cloudera-scm
所有节点都需处理：
~~~
$ useradd --system --home=/opt/cloudera-manager/cm-5.16.1/run/cloudera-scm-server --no-create-home --shell=/bin/false --comment "Cloudera SCM User" cloudera-scm
~~~

### 4.2.4 创建PARCEL目录
创建parcel目录，这个目录是server和agent用来接收和发送数据的目录，server端的parcel-repo这个目录会把所有的安装文件全部下载到此目录，而agent也需要安装包，parcels就是用来存储指定的安装包的，当然需要有权限能操作这些目录。
Server节点
~~~
$ mkdir -p /opt/cloudera/parcel-repo
$ chown cloudera-scm:cloudera-scm /opt/cloudera/parcel-repo
把CDH安装包移到cloudera/parcel-repo下面,并修改其中后缀为sha1的文件为sha。
$ cp -rf cdh/* /opt/cloudera/parcel-repo
~~~
Agent节点
~~~
$ mkdir -p /opt/cloudera/parcels
$ chown cloudera-scm:cloudera-scm /opt/cloudera/parcels
~~~

### 4.2.5 配置CM SERVER的数据库
~~~
# 复制驱动包
$ cp mysql-connector-java-5.1.46.jar /usr/share/java/mysql-connector-java.jar

# 初始化数据库用户
$ /opt/cloudera-manager/cm-5.16.1/share/cmf/schema/scm_prepare_database.sh mysql -h cdh-master  --scm-host cdh-master  cmf cmf cmf
~~~

### 4.2.6 启动SERVER
~~~
$ /opt/cloudera-manager/cm-5.16.1/etc/init.d/cloudera-scm-server start
$ tail -f /opt/cloudera-manager/cm-5.16.1/log/cloudera-scm-server/cloudera-scm-server.log
启动成功后，访问web端，或(netstat -apn|grep 7180)。
如：http://192.168.37.1:7180，用户名和密码默认都是admin
~~~

### 4.2.7 启动AGENT
~~~
$ /opt/cloudera-manager/cm-5.16.1/etc/init.d/cloudera-scm-agent start 
$ tail -f /opt/cloudera-manager/cm-5.16.1/log/cloudera-scm-agent/cloudera-scm-agent.log
查看agent状态：
$ /opt/cloudera-manager/cm-5.16.1/etc/init.d/cloudera-scm-agent status
~~~

### 4.2.8 CM安装
略。网络上有比较多的资料，大同小异，可参考[cdh5.14 单节点parcel方式安装（多图）](https://blog.csdn.net/kyle0349/article/details/82532229)

## 4.3 KAFKA安装
安装4.0.0版本

### 4.3.1 上传安装包
~~~
$ cp KAFKA-4.0.0-1.4.0.0.p0.1-el7.parcel /opt/cloudera/parcel-repo
$ cp KAFKA-4.0.0-1.4.0.0.p0.1-el7.parcel.sha /opt/cloudera/parcel-repo
$ cp manifest.json /opt/cloudera/parcel-repo
$ cp KAFKA-1.2.0.jar /opt/cloudera/csd
~~~

### 4.3.2 CM添加服务
~~~
添加服务第3步，其余全是默认步骤：
Destination Broker List： cdh-master:9092
Source Broker List：      cdh-master:9092
添加服务成功后，配置jvm内存：
Java Heap Size of Broker: 256M
~~~

### 4.3.3 修改配置
~~~
注意CM的配置会覆盖/opt/cloudera/parcels/KAFKA-4.0.0-1.4.0.0.p0.1/etc/kafka/conf.dist的配置
$ vi server.properties
broker.id=1，其他两个为2、3
port=9092，其他两个为9093、9094
host.name=xx.xx.xx.xx，每个配置文件的这项改为本服务器的实际IP/hostname
zookeeper.connect=xx.xx.xx.xx:2181,xx.xx.xx.xx:2181,xx.xx.xx.xx:2181，此处填写实际zookeeper的配置信息

listeners=PLAINTEXT://cdh-master:9092
offsets.topic.replication.factor
~~~

### 4.3.4 测试
~~~
# 创建topic
$ /opt/cloudera/parcels/KAFKA-4.0.0-1.4.0.0.p0.1/lib/kafka/bin/kafka-topics.sh --create --zookeeper cdh-master:2181 --replication-factor 1 --partitions 1 --topic helloworld

# 创建生产者
$ cd /opt/cloudera/parcels/KAFKA-4.0.0-1.4.0.0.p0.1/lib/kafka/bin/ 
./kafka-console-producer.sh --broker-list cdh-master:9092 --topic helloworld
./kafka-console-producer.sh --broker-list cdh-master:9092 -topic helloworld

# 创建消费者
$ cd /opt/cloudera/parcels/KAFKA-4.0.0-1.4.0.0.p0.1/lib/kafka/bin/
./kafka-console-consumer.sh --bootstrap-server cdh-master:9092 --from-beginning --topic helloworld

# 查看topic状态
$ /opt/cloudera/parcels/KAFKA-4.0.0-1.4.0.0.p0.1/lib/kafka/bin/kafka-topics.sh --list --zookeeper cdh-master:2181
$ /opt/cloudera/parcels/KAFKA-4.0.0-1.4.0.0.p0.1/lib/kafka/bin/kafka-topics.sh --describe --zookeeper cdh-master:2181 --topic helloworld
~~~

## 4.4 SPARK2安装
安装前可以停掉集群和Cloudera Management Service，也可以不停，但是待会还是要停止重启的。

### 4.4.1 上传CSD包
~~~
$ cp SPARK2_ON_YARN-2.3.0.cloudera3.jar /opt/cloudera/csd/
$ cd /opt/cloudera/csd && chown cloudera-scm:cloudera-scm SPARK2_ON_YARN-2.3.0.cloudera3.jar
~~~

### 4.4.2 上传parcel包
~~~
$ cp SPARK2-2.3.0.cloudera3-1.cdh5.13.3.p0.458809-el7.parcel  /opt/cloudera/parcel-repo
$ cp  SPARK2-2.3.0.cloudera3-1.cdh5.13.3.p0.458809-el7.parcel.sha  /opt/cloudera/parcel-repo
$ rm -f /opt/cloudera/parcel-repo/manifest.json
$ cp manifest.json /opt/cloudera/parcel-repo
~~~

### 4.4.3 启动CM
CM添加parcel，然后添加服务，可参考[在CDH5.14上离线安装Spark2.3](https://blog.csdn.net/lichangzai/article/details/82225494)

### 4.4.4 spark2的配置
不配置会报slf4j的jar找不到错误
~~~
$ cp /opt/cloudera/parcels/CDH-5.16.1-1.cdh5.16.1.p0.3/etc/spark/conf.dist/*  /opt/cloudera/parcels/SPARK2/etc/spark2/conf.dist/
$ vim /opt/cloudera/parcels/SPARK2/etc/spark2/conf.dist/spark-env.sh
export SPARK_DIST_CLASSPATH=$(hadoop classpath):$SPARK_DIST_CLASSPATH
测试spark2：$ /opt/cloudera/parcels/SPARK2/bin/spark2-shell
~~~
