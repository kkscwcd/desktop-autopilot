# Smart Mouse Jiggler

Small Java 21 application that nudges the mouse by 2 pixels every 6 seconds.

The movement is bounded to the current screen's safe area. When the pointer is near an edge, the app reverses direction and clamps the target point so the cursor is not moved outside the visible desktop area.

## Build

```sh
mvn clean package
```

## Run

```sh
mvn exec:java
```

Or run the packaged jar:

```sh
java -jar target/mouse-jiggler-1.0.0.jar
```

Press `Ctrl+C` to stop.

On macOS, Java may need Accessibility/Input Monitoring permission before it can move the mouse.

## Test

```sh
mvn test
```
