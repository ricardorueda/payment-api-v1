#!/bin/bash

echo "========================================="
echo "Building Payments API..."
echo "========================================="

mvn clean install -DskipTests

if [ $? -eq 0 ]; then
    echo ""
    echo "========================================="
    echo "Build successful! Starting application..."
    echo "========================================="
    echo ""
    mvn spring-boot:run
else
    echo ""
    echo "========================================="
    echo "Build failed! Please check the errors above."
    echo "========================================="
    exit 1
fi

