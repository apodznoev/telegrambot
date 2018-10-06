#!/bin/bash
sudo su
echo 'That works and hello there!' >> /home/ec2-user/hello.txt
yum update -y
yum install java -y
yum install -y amazon-efs-utils -y
mkdir /home/ec2-user/telegram-bot
touch /etc/init.d/telegram-bot
chmod +x /etc/init.d/telegram-bot
echo "#!/bin/bash" >> /etc/init.d/telegram-bot
echo "aws s3 cp s3://avpod/telegram-bot.jar /home/ec2-user/telegram-bot" >> /etc/init.d/telegram-bot
echo "PUBLIC_HOST="$(ec2-metadata --public-hostname | tr -s ' ' | cut -d ' ' -f 2)"" >> /etc/init.d/telegram-bot
echo "java -jar -Dspring.profiles.active=prod -Dgoogle.oauth.host=$PUBLIC_HOST /home/ec2-user/telegram-bot/telegram-bot.jar > /home/ec2-user/telegram-bot/app.log | echo $! > /home/ec2-user/telegram-bot/app.pid &" >> /etc/init.d/telegram-bot
echo "service telegram-bot start" >> /etc/rc.local
service telegram-bot start

