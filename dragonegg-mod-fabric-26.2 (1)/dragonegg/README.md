# Dragones Elementales (Fabric 26.2)

Como usarlo:
1. Instala JDK 25.
2. Genera un proyecto base en https://fabricmc.net/develop/template/ (Minecraft 26.2, mod id `dragonegg`, paquete `com.dragonegg`).
3. Copia la carpeta `src` de este zip encima de la del proyecto base (sobrescribe).
   Si no quieres usar la plantilla, este zip trae build.gradle y gradle.properties, pero te faltara el gradle wrapper.
4. `./gradlew build` -> el .jar queda en build/libs. Para probar: `./gradlew runClient`.
5. Para probar rapido sin buscar el huevo: /give @s dragonegg:elemental_egg
