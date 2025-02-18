# bash IoTDB+align+SINGLE_SERIES_COUNT_QUERY.sh > IoTDB+align+SINGLE_SERIES_COUNT_QUERY.txt

#for i in 1 100 10000 1000000 # check check check !
#for i in 1 10000 1000000 100000000
for i in 10000 100000 1000000 10000000 100000000
do
	for (( j = 1 ; j <= 6; j++ ))
	do
		echo "Query Param=$i, loop=$j/6"
                #停止服务器
                ps -ef|grep 'iotdb'|awk '{print $2}'|xargs kill -9
		sleep 60s

                echo 3 |sudo tee /proc/sys/vm/drop_caches 
                free -m
                sleep 2s

                #开启服务器
                cd /data/dbms/iotdb-nonalign/iotdb-server-0.14.0-SNAPSHOT/sbin # nonalign要改路径！！！！check!!!
                nohup ./start-server.sh &
                sleep 120s #这个不能太小，否则服务器还没启动完毕后面就查询了，调大是比较安全的
                
                cd /data/dbms/BenchTSDB
                java -jar /data/dbms/BenchTSDB/BenchTSDB-1.0-jar-with-dependencies.jar -r "/data/dbms/BenchTSDB/configs/iotdb/IoTDB+SINGLE_SERIES_COUNT_QUERY_$i.properties" #note this should NOT be made background !!!!!!
        done
done

#停止服务器
ps -ef|grep 'iotdb'|awk '{print $2}'|xargs kill -9
sleep 60s

ps -ef | grep iotdb
echo 3 |sudo tee /proc/sys/vm/drop_caches 
free -m

echo "ALL DONE!"
