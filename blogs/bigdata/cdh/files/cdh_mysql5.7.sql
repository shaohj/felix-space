create database cmf DEFAULT CHARACTER SET utf8 COLLATE utf8_general_ci;
insert into mysql.user(Host,User,authentication_string,ssl_cipher,x509_issuer,x509_subject) values("%","cmf",password("cmf"),"1","1","1");
flush privileges;
grant all privileges on cmf.* to cmf@'%' identified by 'cmf';
grant all privileges on cmf.* to cmf@'cdh01' identified by 'cmf';

create database metastore DEFAULT CHARACTER SET utf8 COLLATE utf8_general_ci;
insert into mysql.user(Host,User,authentication_string,ssl_cipher,x509_issuer,x509_subject) values("%","metastore",password("metastore"),"1","1","1");
flush privileges;
grant all privileges on metastore.* to metastore@'%' identified by 'metastore';
grant all privileges on metastore.* to metastore@'cdh01' identified by 'metastore';

create database oozie DEFAULT CHARSET utf8 COLLATE utf8_general_ci;
insert into mysql.user(Host,User,authentication_string,ssl_cipher,x509_issuer,x509_subject) values("%","oozie",password("oozie"),"1","1","1");
flush privileges;
grant all privileges on oozie.* to oozie@'%' identified by 'oozie';
grant all privileges on oozie.* to oozie@'cdh01' identified by 'oozie';

create database hive DEFAULT CHARSET utf8 COLLATE utf8_general_ci;
insert into mysql.user(Host,User,authentication_string,ssl_cipher,x509_issuer,x509_subject) values("%","hive",password("hive"),"1","1","1");
flush privileges;
grant all privileges on hive.* to hive@'%' identified by 'hive';
grant all privileges on hive.* to hive@'cdh01' identified by 'hive';

create database hue DEFAULT CHARSET utf8 COLLATE utf8_general_ci;
insert into mysql.user(Host,User,authentication_string,ssl_cipher,x509_issuer,x509_subject) values("%","hue",password("hue"),"1","1","1");
flush privileges;
grant all privileges on hue.* to hue@'%' identified by 'hue';
grant all privileges on hue.* to hue@'cdh01' identified by 'hue';

create database monitor DEFAULT CHARSET utf8 COLLATE utf8_general_ci;
insert into mysql.user(Host,User,authentication_string,ssl_cipher,x509_issuer,x509_subject) values("%","monitor",password("monitor"),"1","1","1");
flush privileges;
grant all privileges on monitor.* to monitor@'%' identified by 'monitor';
grant all privileges on monitor.* to monitor@'cdh01' identified by 'monitor';

grant all privileges on *.* to 'root'@'cdh01' identified by 'root' with grant option;
grant all privileges on *.* to 'root'@'%' identified by 'root' with grant option;
flush privileges;