echo '' > /etc/apt/sources.list
echo 'deb https://mirrors.ustc.edu.cn/ubuntu/ bionic main restricted universe multiverse' >> /etc/apt/sources.list
echo 'deb https://mirrors.ustc.edu.cn/ubuntu/ bionic-updates main restricted universe multiverse' >> /etc/apt/sources.list
echo 'deb https://mirrors.ustc.edu.cn/ubuntu/ bionic-backports main restricted universe multiverse' >> /etc/apt/sources.list
echo 'deb https://mirrors.ustc.edu.cn/ubuntu/ bionic-security main restricted universe multiverse' >> /etc/apt/sources.list
echo 'deb https://mirrors.ustc.edu.cn/ubuntu/ bionic-proposed main restricted universe multiverse' >> /etc/apt/sources.list
echo 'deb-src https://mirrors.ustc.edu.cn/ubuntu/ bionic main restricted universe multiverse' >> /etc/apt/sources.list
echo 'deb-src https://mirrors.ustc.edu.cn/ubuntu/ bionic-updates main restricted universe multiverse' >> /etc/apt/sources.list
echo 'deb-src https://mirrors.ustc.edu.cn/ubuntu/ bionic-backports main restricted universe multiverse' >> /etc/apt/sources.list
echo 'deb-src https://mirrors.ustc.edu.cn/ubuntu/ bionic-security main restricted universe multiverse' >> /etc/apt/sources.list
echo 'deb-src https://mirrors.ustc.edu.cn/ubuntu/ bionic-proposed main restricted universe multiverse' >> /etc/apt/sources.list
apt-get update
#apt-get install -y libtinfo5 libpython2.7 vim -y --allow-remove-essential
apt-get install -y vim wget -y --allow-remove-essential