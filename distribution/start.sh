 nohup java -Xms256m -Xmx256m -jar /device_server-1.0-SNAPSHOT.jar  --spring.profiles.active="$PROFILE" > /dev/null 2>&1 &

 nohup java -Xms256m -Xmx256m -jar /device_Gateway-1.0-SNAPSHOT.jar  --spring.profiles.active="$PROFILE" > /dev/null 2>&1 &

 nohup java -Xms512m -Xmx512m -jar /device_Data-1.0-SNAPSHOT.jar --spring.profiles.active="$PROFILE" > /dev/null 2>&1 &

 nohup java -Xms512m -Xmx512m -jar /device_user-1.0-SNAPSHOT.jar --spring.profiles.active="$PROFILE" > /dev/null 2>&1 &

 tail -f /dev/null