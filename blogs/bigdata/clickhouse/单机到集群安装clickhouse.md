# 1 单机到集群安装CLICKHOUSE20.3.5.21-2
clickhouse20.3.5.21-2单机到集群安装，通过VM虚拟机安装，可离线安装。

## 1.1 文档规范
* 代码块和vi编辑块用灰色区域标记
* $后跟的linux命令

# 2 安装准备
操作系统：CentOS7.5  
使用官方预编译的rpm软件包安装，预构建的rpm文件要求CPU必须支持SSE 4.2指令集，以下命令是检查当前CPU是否支持SSE 4.2的命令
~~~
$ grep -q sse4_2 /proc/cpuinfo && echo "SSE 4.2 supported" || echo "SSE 4.2 not supported"
~~~

## 2.1 资源清单
[官方包下载传送门](https://repo.yandex.ru/clickhouse/rpm/stable/x86_64/)  
注意clickhouse有部分基础依赖，随操作系统具体情况安装，即安装时提示少某些基础依赖时，下载对应的rpm离线安装或yum在线安装即可，以下是"CentOS-7-x86_64-Everything-1804.iso"安装时需要的rpm安装包，其中server组件是必须安装的，其他两个可选安装。

| Type | Name | notes |
| :-----| :---- | :---- |
| clickhouse-server | clickhouse-common-static-20.3.5.21-2.x86_64.rpm | Server端，也就是CK数据库的核心程序，相当于mysqld命令，提供数据库服务端 |
| clickhouse-server | clickhouse-common-static-dbg-20.3.5.21-2.x86_64.rpm | |
| clickhouse-server | clickhouse-server-20.3.5.21-2.noarch.rpm | |
| clickhouse-client | clickhouse-client-20.3.5.21-2.noarch.rpm | client端，提供命令行的交互操作方式，来连接服务端，相当于mysql命令 |
| clickhouse-test   | clickhouse-test-20.3.5.21-2.noarch.rpm | |
| 基础依赖 | perl-common-sense-3.6-4.el7.noarch.rpm | clickhouse-test基础依赖 |
| 基础依赖 | perl-JSON-XS-3.01-2.el7.x86_64.rpm | clickhouse-test基础依赖 |
| 基础依赖 | perl-Types-Serialiser-1.0-1.el7.noarch.rpm | clickhouse-test基础依赖 |

## 3 单机安装
如果是单机安装，可跳过集群安装小节；如果是集群安装，安装好单机环境后，还要继续进行集群安装和配置。

### 3.1 RPM安装
按照以下顺序安装rpm即可，安装后，clickhouse会自动开机重启
~~~
# server安装
$ rpm -ivh clickhouse-common-static-20.3.5.21-2.x86_64.rpm
$ rpm -ivh clickhouse-common-static-dbg-20.3.5.21-2.x86_64.rpm
$ rpm -ivh clickhouse-server-20.3.5.21-2.noarch.rpm

# client安装
$ rpm -ivh clickhouse-client-20.3.5.21-2.noarch.rpm

# test安装
$ rpm -ivh perl-common-sense-3.6-4.el7.noarch.rpm
$ rpm -ivh perl-Types-Serialiser-1.0-1.el7.noarch.rpm
$ rpm -ivh perl-JSON-XS-3.01-2.el7.x86_64.rpm
$ rpm -ivh clickhouse-test-20.3.5.21-2.noarch.rpm
~~~

### 3.2 修改配置
clickHouse有几个核心的配置文件：
* config.xml 端口配置、本地机器名配置、内存设置等
* users.xml 权限、配额设置
* metrika.xml 集群配置、ZK配置、分片配置等，配置集群时说明具体的配置

修改配置举例如下
* 开放ip远程访问，(方法不唯一，如还可以设置另外的接收ip参数为0.0.0.0)
~~~ 
$ vi /etc/clickhouse-server/config.xml
  <listen_host>::</listen_host>
~~~ 
* 修改用户缓存，默认是10G，我们按照下面方案改为20G
~~~ 
$ vi /etc/clickhouse-server/users.xml
  修改<max_memory_usage>10000000000</max_memory_usage>为20000000000
~~~

### 3.3 常用命令
~~~
# 启动
$ service clickhouse-server start

# 停止
$ service clickhouse-server stop

# 强制停止server进程
有时使用停止命令无法停止时，可使用$ killall clickhouse-server强制停止所有clickhouse-server进程

# 查看server日志
tail -50f /var/log/clickhouse-server/clickhouse-server.log 

# 查看状态
cat /data/clickhouse/status 
~~~

### 3.4 客户端连接服务器
clickhouse-client --host=localhost 
> select 1

## 4 集群安装

## 4.1 节点分配
节点分配如下表  

| 节点  | 组件 |
| :-----| ----: |
| ck01  | server、client |
| ck02  | server |
| ck03  | server |

## 4.2 网络配置

### 4.2.1 VM配置固定IP
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

### 4.2.2 设置主机名和IP
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
192.168.37.1  ck01
192.168.37.2  ck02
192.168.37.3  ck03

~~~

### 4.3 安装JDK
~~~
zk需要安装jdk，我们不使用系统自带的openjdk，安装下载好的jdk1.8。
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

### 4.4 关闭防火墙和关闭SELINUX
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

## 4.5 ZOOKEEPER集群安装

### 4.5.1 ZOOKEEPER集群安装
安装步骤如下
~~~
# 首先创建Zookeeper项目目录、存放快照日志、存放事务日志
mkdir /opt/zookeeper && mkdir /opt/zookeeper/zkdata && mkdir /opt/zookeeper/zkdatalog

# 解压zookeeper
tar -zxvf zookeeper-3.4.6.tar.gz -C /opt/zookeeper

# 配置环境变量
$ vi /etc/profile
export ZOOKEEPER_HOME=/opt/zookeeper/zookeeper-3.4.6
export PATH=$PATH:$ZOOKEEPER_HOME/bin:$ZOOKEEPER_HOME/conf
$ source /etc/profile

# 修改配置文件
cd /opt/zookeeper/zookeeper-3.4.6/conf
cp zoo_sample.cfg zoo.cfg && vi zoo.cfg

tickTime=2000
initLimit=10
syncLimit=5
dataDir=/opt/zookeeper/zkdata
dataLogDir=/opt/zookeeper/zkdatalog
clientPort=2181
server.1=server1:2888:3888
server.2=server2:2888:3888
server.3=server3:2888:3888

# 创建myid文件
# server1
echo "1" > /opt/zookeeper/zkdata/myid
# server2
echo "2" > /opt/zookeeper/zkdata/myid
# server3
echo "3" > /opt/zookeeper/zkdata/myid

# 启动3台zookeeper服务
zkServer.sh start

# 检查服务器状态
zkServer.sh status

# 可以用“jps”查看zk的进程，这个是zk的整个工程的main
$ jps
20348 Jps
4233 QuorumPeerMain 
~~~

### 4.5.2 ZOOKEEPER常用命令
~~~
# 启动
zkServer.sh start
zkServer.sh start-foreground　　//以打印日志方式启动

# 停止
zkServer.sh stop

# 重启
zkServer.sh restart　

# 查看状态
zkServer.sh status
~~~

## 4.6 配置集群

### 4.6.1 config.xml配置
可进行端口配置、本地机器名配置、内存设置等，修改配置如下说明
~~~
$ vi /etc/clickhouse-server/config.xml
# 重要的几个配置描述如下，其他可查看示例配置文件
* 开放ip访问：<listen_host>::</listen_host>

* 修改日志目录，目录不存在需先创建：
<log>/data/clickhouse/logs/server.log</log>
<errorlog>/data/clickhouse/logs/err.log</errorlog>

* 修改数据存储目录，目录不存在需先创建
<path>/data/clickhouse/</path>
<tmp_path>/data/clickhouse/tmp/</tmp_path>

* zookeeper配置及metrika.xml文件路径指定
<zookeeper incl="zookeeper-servers" optional="true" />
<include_from>/data/clickhouse/metrika.xml</include_from>
~~~
实际配置参考：[config.xml](./files/config.xml) 

### 4.6.2 metrika.xml配置
可进行集群配置、ZK配置、分片配置等，具体见[metrika.xml](./files/metrika.xml) 
将文件拷贝至/data/clickhouse/metrika.xml

### 4.6.3 users.xml配置
可进行权限、配额设置。配置用户缓存请参考"3.2 修改配置"小节，其他配置略。

### 4.6.4 修改启动脚本
$ vim /etc/init.d/clickhouse-server
CLICKHOUSE_LOGDIR=/data/clickhouse/logs
 
### 4.6.5 验证
~~~
$ clickhouse-client --host=localhost
# 查看集群
select * from system.clusters;
~~~

## 5 官方示例数据测试
可参考官方教程进行下载数据和测试(数据可找国内的网盘或下载地址下载加速)，[clickhouse官方数据测试参考](https://clickhouse.tech/docs/zh/getting-started/example-datasets/metrica/)

### 5.1 HITS_V1.TAR
导数步骤如下，导入后的database为datasets
~~~
curl -O https://clickhouse-datasets.s3.yandex.net/hits/partitions/hits_v1.tar
tar xvf hits_v1.tar -C /data/clickhouse
service clickhouse-server restart
clickhouse-client --query "SELECT COUNT(*) FROM datasets.hits_v1"
~~~

### 5.2 visits_v1.tar
导数步骤如下，导入后的database为datasets
~~~
curl -O https://clickhouse-datasets.s3.yandex.net/visits/partitions/visits_v1.tar
tar xvf visits_v1.tar -C /data/clickhouse 
service clickhouse-server restart
clickhouse-client --query "SELECT COUNT(*) FROM datasets.visits_v1"
~~~

### 5.3 分布式表测试
在上面两步操作后，数据都在datasets库中，且是单机的表(clickhouse是伪分布式集群，理论略)，我们再创建分布式的表
~~~
# 在三个节点分别建库、建表
create database tutorial;
CREATE TABLE tutorial.hits_local ( `WatchID` UInt64, `JavaEnable` UInt8, `Title` String, `GoodEvent` Int16, `EventTime` DateTime, `EventDate` Date, `CounterID` UInt32, `ClientIP` UInt32, `ClientIP6` FixedString(16), `RegionID` UInt32, `UserID` UInt64, `CounterClass` Int8, `OS` UInt8, `UserAgent` UInt8, `URL` String, `Referer` String, `URLDomain` String, `RefererDomain` String, `Refresh` UInt8, `IsRobot` UInt8, `RefererCategories` Array(UInt16), `URLCategories` Array(UInt16), `URLRegions` Array(UInt32), `RefererRegions` Array(UInt32), `ResolutionWidth` UInt16, `ResolutionHeight` UInt16, `ResolutionDepth` UInt8, `FlashMajor` UInt8, `FlashMinor` UInt8, `FlashMinor2` String, `NetMajor` UInt8, `NetMinor` UInt8, `UserAgentMajor` UInt16, `UserAgentMinor` FixedString(2), `CookieEnable` UInt8, `JavascriptEnable` UInt8, `IsMobile` UInt8, `MobilePhone` UInt8, `MobilePhoneModel` String, `Params` String, `IPNetworkID` UInt32, `TraficSourceID` Int8, `SearchEngineID` UInt16, `SearchPhrase` String, `AdvEngineID` UInt8, `IsArtifical` UInt8, `WindowClientWidth` UInt16, `WindowClientHeight` UInt16, `ClientTimeZone` Int16, `ClientEventTime` DateTime, `SilverlightVersion1` UInt8, `SilverlightVersion2` UInt8, `SilverlightVersion3` UInt32, `SilverlightVersion4` UInt16, `PageCharset` String, `CodeVersion` UInt32, `IsLink` UInt8, `IsDownload` UInt8, `IsNotBounce` UInt8, `FUniqID` UInt64, `HID` UInt32, `IsOldCounter` UInt8, `IsEvent` UInt8, `IsParameter` UInt8, `DontCountHits` UInt8, `WithHash` UInt8, `HitColor` FixedString(1), `UTCEventTime` DateTime, `Age` UInt8, `Sex` UInt8, `Income` UInt8, `Interests` UInt16, `Robotness` UInt8, `GeneralInterests` Array(UInt16), `RemoteIP` UInt32, `RemoteIP6` FixedString(16), `WindowName` Int32, `OpenerName` Int32, `HistoryLength` Int16, `BrowserLanguage` FixedString(2), `BrowserCountry` FixedString(2), `SocialNetwork` String, `SocialAction` String, `HTTPError` UInt16, `SendTiming` Int32, `DNSTiming` Int32, `ConnectTiming` Int32, `ResponseStartTiming` Int32, `ResponseEndTiming` Int32, `FetchTiming` Int32, `RedirectTiming` Int32, `DOMInteractiveTiming` Int32, `DOMContentLoadedTiming` Int32, `DOMCompleteTiming` Int32, `LoadEventStartTiming` Int32, `LoadEventEndTiming` Int32, `NSToDOMContentLoadedTiming` Int32, `FirstPaintTiming` Int32, `RedirectCount` Int8, `SocialSourceNetworkID` UInt8, `SocialSourcePage` String, `ParamPrice` Int64, `ParamOrderID` String, `ParamCurrency` FixedString(3), `ParamCurrencyID` UInt16, `GoalsReached` Array(UInt32), `OpenstatServiceName` String, `OpenstatCampaignID` String, `OpenstatAdID` String, `OpenstatSourceID` String, `UTMSource` String, `UTMMedium` String, `UTMCampaign` String, `UTMContent` String, `UTMTerm` String, `FromTag` String, `HasGCLID` UInt8, `RefererHash` UInt64, `URLHash` UInt64, `CLID` UInt32, `YCLID` UInt64, `ShareService` String, `ShareURL` String, `ShareTitle` String, `ParsedParams` Nested( Key1 String, Key2 String, Key3 String, Key4 String, Key5 String, ValueDouble Float64), `IslandID` FixedString(16), `RequestNum` UInt32, `RequestTry` UInt8 ) ENGINE = MergeTree() PARTITION BY toYYYYMM(EventDate) ORDER BY (CounterID, EventDate, intHash32(UserID)) SAMPLE BY intHash32(UserID) SETTINGS index_granularity = 8192;
CREATE TABLE tutorial.visits_local ( `CounterID` UInt32, `StartDate` Date, `Sign` Int8, `IsNew` UInt8, `VisitID` UInt64, `UserID` UInt64, `StartTime` DateTime, `Duration` UInt32, `UTCStartTime` DateTime, `PageViews` Int32, `Hits` Int32, `IsBounce` UInt8, `Referer` String, `StartURL` String, `RefererDomain` String, `StartURLDomain` String, `EndURL` String, `LinkURL` String, `IsDownload` UInt8, `TraficSourceID` Int8, `SearchEngineID` UInt16, `SearchPhrase` String, `AdvEngineID` UInt8, `PlaceID` Int32, `RefererCategories` Array(UInt16), `URLCategories` Array(UInt16), `URLRegions` Array(UInt32), `RefererRegions` Array(UInt32), `IsYandex` UInt8, `GoalReachesDepth` Int32, `GoalReachesURL` Int32, `GoalReachesAny` Int32, `SocialSourceNetworkID` UInt8, `SocialSourcePage` String, `MobilePhoneModel` String, `ClientEventTime` DateTime, `RegionID` UInt32, `ClientIP` UInt32, `ClientIP6` FixedString(16), `RemoteIP` UInt32, `RemoteIP6` FixedString(16), `IPNetworkID` UInt32, `SilverlightVersion3` UInt32, `CodeVersion` UInt32, `ResolutionWidth` UInt16, `ResolutionHeight` UInt16, `UserAgentMajor` UInt16, `UserAgentMinor` UInt16, `WindowClientWidth` UInt16, `WindowClientHeight` UInt16, `SilverlightVersion2` UInt8, `SilverlightVersion4` UInt16, `FlashVersion3` UInt16, `FlashVersion4` UInt16, `ClientTimeZone` Int16, `OS` UInt8, `UserAgent` UInt8, `ResolutionDepth` UInt8, `FlashMajor` UInt8, `FlashMinor` UInt8, `NetMajor` UInt8, `NetMinor` UInt8, `MobilePhone` UInt8, `SilverlightVersion1` UInt8, `Age` UInt8, `Sex` UInt8, `Income` UInt8, `JavaEnable` UInt8, `CookieEnable` UInt8, `JavascriptEnable` UInt8, `IsMobile` UInt8, `BrowserLanguage` UInt16, `BrowserCountry` UInt16, `Interests` UInt16, `Robotness` UInt8, `GeneralInterests` Array(UInt16), `Params` Array(String), `Goals` Nested( ID UInt32, Serial UInt32, EventTime DateTime, Price Int64, OrderID String, CurrencyID UInt32), `WatchIDs` Array(UInt64), `ParamSumPrice` Int64, `ParamCurrency` FixedString(3), `ParamCurrencyID` UInt16, `ClickLogID` UInt64, `ClickEventID` Int32, `ClickGoodEvent` Int32, `ClickEventTime` DateTime, `ClickPriorityID` Int32, `ClickPhraseID` Int32, `ClickPageID` Int32, `ClickPlaceID` Int32, `ClickTypeID` Int32, `ClickResourceID` Int32, `ClickCost` UInt32, `ClickClientIP` UInt32, `ClickDomainID` UInt32, `ClickURL` String, `ClickAttempt` UInt8, `ClickOrderID` UInt32, `ClickBannerID` UInt32, `ClickMarketCategoryID` UInt32, `ClickMarketPP` UInt32, `ClickMarketCategoryName` String, `ClickMarketPPName` String, `ClickAWAPSCampaignName` String, `ClickPageName` String, `ClickTargetType` UInt16, `ClickTargetPhraseID` UInt64, `ClickContextType` UInt8, `ClickSelectType` Int8, `ClickOptions` String, `ClickGroupBannerID` Int32, `OpenstatServiceName` String, `OpenstatCampaignID` String, `OpenstatAdID` String, `OpenstatSourceID` String, `UTMSource` String, `UTMMedium` String, `UTMCampaign` String, `UTMContent` String, `UTMTerm` String, `FromTag` String, `HasGCLID` UInt8, `FirstVisit` DateTime, `PredLastVisit` Date, `LastVisit` Date, `TotalVisits` UInt32, `TraficSource` Nested( ID Int8, SearchEngineID UInt16, AdvEngineID UInt8, PlaceID UInt16, SocialSourceNetworkID UInt8, Domain String, SearchPhrase String, SocialSourcePage String), `Attendance` FixedString(16), `CLID` UInt32, `YCLID` UInt64, `NormalizedRefererHash` UInt64, `SearchPhraseHash` UInt64, `RefererDomainHash` UInt64, `NormalizedStartURLHash` UInt64, `StartURLDomainHash` UInt64, `NormalizedEndURLHash` UInt64, `TopLevelDomain` UInt64, `URLScheme` UInt64, `OpenstatServiceNameHash` UInt64, `OpenstatCampaignIDHash` UInt64, `OpenstatAdIDHash` UInt64, `OpenstatSourceIDHash` UInt64, `UTMSourceHash` UInt64, `UTMMediumHash` UInt64, `UTMCampaignHash` UInt64, `UTMContentHash` UInt64, `UTMTermHash` UInt64, `FromHash` UInt64, `WebVisorEnabled` UInt8, `WebVisorActivity` UInt32, `ParsedParams` Nested( Key1 String, Key2 String, Key3 String, Key4 String, Key5 String, ValueDouble Float64), `Market` Nested( Type UInt8, GoalID UInt32, OrderID String, OrderPrice Int64, PP UInt32, DirectPlaceID UInt32, DirectOrderID UInt32, DirectBannerID UInt32, GoodID String, GoodName String, GoodQuantity Int32, GoodPrice Int64), `IslandID` FixedString(16) ) ENGINE = CollapsingMergeTree(Sign) PARTITION BY toYYYYMM(StartDate) ORDER BY (CounterID, StartDate, intHash32(UserID), VisitID) SAMPLE BY intHash32(UserID) SETTINGS index_granularity = 8192;

# 建分布表，一个节点执行即可
CREATE TABLE tutorial.hits_all ON CLUSTER ck_clusters AS tutorial.hits_local ENGINE = Distributed(ck_clusters, tutorial, hits_local, rand());
CREATE TABLE tutorial.visits_all ON CLUSTER ck_clusters AS tutorial.visits_local ENGINE = Distributed(ck_clusters, tutorial, visits_local, rand());

# 分布式表导入数据
INSERT INTO tutorial.hits_all SELECT * FROM datasets.hits_v1;
INSERT INTO tutorial.visits_all SELECT * FROM datasets.visits_local;

# 性能测试
略。
~~~

## 6 其他

### 6.1 CLICKHOUSE-CLIENT快速导数
clickhouse使用sql插入数据时，会先将数据缓存到临时缓存分区中，再定时合并分区(大概是10分钟)，因此sql导数非常容易报错，我们可使用clickhouse-client将csv文件快速导数。如下
~~~
# x.csv
'str1','str2',1,...

# 导入csv数据
cat x.csv | clickhouse-client --host=localhost --port=9000 --query="INSERT INTO db.table FORMAT CSV"
~~~
