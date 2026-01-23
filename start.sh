ln -s /home/ben/Documents/AutomatApp/start.sh /home/ben/Desktop/start
cd /home/ben/Documents/AutomatApp
git pull
sudo apt install tmux -y
sudo apt install openjdk-21-jdk -y
./mvnw clean install
tmux new-session -d -s javaBackend
tmux send-keys -t javaBackend "cd /home/ben/Documents/AutomatApp" c-m
tmux send-keys -t javaBackend "./mvnw clean install javafx:run" c-m
