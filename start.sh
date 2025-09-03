#!/bin/bash

echo "🏦 银行交易管理系统启动脚本"
echo "================================"

# 检查Java版本
echo "检查Java版本..."
if ! command -v java &> /dev/null; then
    echo "❌ 错误: 未找到Java运行时环境"
    echo "请安装Java 17或更高版本"
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 17 ]; then
    echo "❌ 错误: Java版本过低 (当前: $JAVA_VERSION, 需要: 17+)"
    exit 1
fi

echo "✅ Java版本检查通过: $(java -version 2>&1 | head -n 1)"

# 检查Maven
echo "检查Maven..."
if ! command -v mvn &> /dev/null; then
    echo "⚠️  警告: 未找到Maven，将使用Maven Wrapper"
    if [ ! -f "./mvnw" ]; then
        echo "❌ 错误: 未找到Maven Wrapper"
        echo "请确保mvnw文件存在"
        exit 1
    fi
    MAVEN_CMD="./mvnw"
else
    MAVEN_CMD="mvn"
    echo "✅ Maven检查通过: $(mvn -version | head -n 1)"
fi

# 清理和编译
echo "🧹 清理项目..."
$MAVEN_CMD clean

echo "🔨 编译项目..."
$MAVEN_CMD compile

if [ $? -ne 0 ]; then
    echo "❌ 编译失败"
    exit 1
fi

echo "✅ 编译成功"

# 运行测试
echo "🧪 运行测试..."
$MAVEN_CMD test

if [ $? -ne 0 ]; then
    echo "⚠️  测试失败，但继续启动应用..."
else
    echo "✅ 测试通过"
fi

# 启动应用
echo "🚀 启动应用..."
echo "应用将在 http://localhost:8080 启动"
echo "H2数据库控制台: http://localhost:8080/h2-console"
echo "Web界面: http://localhost:8080"
echo ""
echo "按 Ctrl+C 停止应用"
echo "================================"

$MAVEN_CMD spring-boot:run

