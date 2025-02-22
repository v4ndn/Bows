
mvn package -f "./bows/pom.xml"
cp "./bows/target/bows-1.0.jar" "./server/plugins"; 
echo "Builded API"


./rconreload -addr localhost -cmd "plugman reload Bows" -port 25575 -pwd 1231

# cd server
# java -jar spigot-1.21.4.jar --nogui
