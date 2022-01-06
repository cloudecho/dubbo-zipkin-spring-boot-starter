package:
	mvn -DskipTests clean package

publish:
	mvn -DskipTests clean deploy
