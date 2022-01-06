package:
	mvn -DskipTests clean package

install:
	mvn -DskipTests -Psonatype-oss-release clean install

publish:
	mvn -DskipTests -Psonatype-oss-release clean deploy
