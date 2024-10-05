# Variables
SRC_DIR = src
OUT_DIR = out
LIBS = lib/*
JAVAC = javac
JAVA = java
CP = -cp "$(LIBS);$(OUT_DIR)"

# Source files
SOURCES = $(wildcard $(SRC_DIR)/com/weather/*.java)

# (compile all Java files)
all: $(OUT_DIR)/com/weather/AggregationServer.class $(OUT_DIR)/com/weather/ContentServer.class $(OUT_DIR)/com/weather/GETClient.class

# Compile the Java files
$(OUT_DIR)/com/weather/%.class: $(SRC_DIR)/com/weather/%.java
	$(JAVAC) -cp "$(LIBS);$(SRC_DIR)" $< -d $(OUT_DIR)

# Run the Aggregation Server
run-aggregation-server: all
	$(JAVA) $(CP) com.weather.AggregationServer

# Run the Content Server
run-content-server: all
	$(JAVA) $(CP) com.weather.ContentServer http://localhost:4567 weather_data.txt
	$(JAVA) $(CP) com.weather.ContentServer http://localhost:4567 weather_data2.txt
	$(JAVA) $(CP) com.weather.ContentServer http://localhost:4567 weather_data3.txt
# Run the GET Client
run-get-client: all
	$(JAVA) $(CP) com.weather.GETClient http://localhost:4567

# Clean the compiled classes
clean:
	rm -rf $(OUT_DIR)/com/weather/*.class

# Phony targets (targets that don't create files)
.PHONY: all run-aggregation-server run-content-server run-get-client clean
