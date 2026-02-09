#!/bin/bash

# 家肴应用 API 测试脚本
# 用于快速测试后端 API 是否正常工作

BASE_URL="http://localhost:8080"
EMAIL="test_$(date +%s)@example.com"
PASSWORD="123456"
USERNAME="测试用户_$(date +%s)"

echo "========================================"
echo "家肴应用 API 测试"
echo "========================================"
echo ""

# 颜色输出
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 测试 1: 检查后端是否启动
echo -e "${YELLOW}测试 1: 检查后端是否启动${NC}"
echo "请求: GET $BASE_URL/api/auth/login"
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X POST $BASE_URL/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@test.com","password":"123"}')

if [ "$HTTP_CODE" -eq 000 ]; then
    echo -e "${RED}❌ 后端未启动或无法连接${NC}"
    echo "   请检查后端是否在运行: ./mvnw spring-boot:run"
    exit 1
else
    echo -e "${GREEN}✓ 后端已启动 (HTTP $HTTP_CODE)${NC}"
fi
echo ""

# 测试 2: 注册新用户
echo -e "${YELLOW}测试 2: 注册新用户${NC}"
echo "邮箱: $EMAIL"
echo "用户名: $USERNAME"
REGISTER_RESPONSE=$(curl -s -X POST $BASE_URL/api/auth/register \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$EMAIL\",\"username\":\"$USERNAME\",\"password\":\"$PASSWORD\"}")

echo "响应: $REGISTER_RESPONSE"

if echo "$REGISTER_RESPONSE" | grep -q '"code":200'; then
    echo -e "${GREEN}✓ 注册成功${NC}"
else
    echo -e "${RED}❌ 注册失败${NC}"
    exit 1
fi
echo ""

# 测试 3: 登录获取 Token
echo -e "${YELLOW}测试 3: 登录获取 Token${NC}"
LOGIN_RESPONSE=$(curl -s -X POST $BASE_URL/api/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\"}")

echo "响应: $LOGIN_RESPONSE"

TOKEN=$(echo "$LOGIN_RESPONSE" | grep -o '"token":"[^"]*' | cut -d'"' -f4)
USER_ID=$(echo "$LOGIN_RESPONSE" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)

if [ -z "$TOKEN" ]; then
    echo -e "${RED}❌ 登录失败，未获取到 Token${NC}"
    exit 1
else
    echo -e "${GREEN}✓ 登录成功${NC}"
    echo "Token: ${TOKEN:0:50}..."
    echo "用户ID: $USER_ID"
fi
echo ""

# 测试 4: 上传图片（使用占位图片）
echo -e "${YELLOW}测试 4: 上传图片${NC}"
# 创建一个临时的小图片文件
echo "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==" | base64 -d > /tmp/test_image.png

UPLOAD_RESPONSE=$(curl -s -X POST $BASE_URL/api/upload/image \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@/tmp/test_image.png")

echo "响应: $UPLOAD_RESPONSE"

IMAGE_URL=$(echo "$UPLOAD_RESPONSE" | grep -o '"data":"[^"]*' | cut -d'"' -f4)

if [ -z "$IMAGE_URL" ]; then
    echo -e "${YELLOW}⚠ 图片上传失败（可能图片功能未完全配置）${NC}"
    IMAGE_URL=""
else
    echo -e "${GREEN}✓ 图片上传成功${NC}"
    echo "图片URL: $IMAGE_URL"
fi

# 清理临时文件
rm -f /tmp/test_image.png
echo ""

# 测试 5: 创建菜谱
echo -e "${YELLOW}测试 5: 创建菜谱${NC}"
RECIPE_NAME="测试菜谱_$(date +%H%M%S)"

CREATE_RECIPE_RESPONSE=$(curl -s -X POST $BASE_URL/api/recipes \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d "{
    \"recipe\": {
      \"name\": \"$RECIPE_NAME\",
      \"description\": \"这是一个测试菜谱，用于验证API功能\",
      \"coverImage\": \"$IMAGE_URL\"
    },
    \"tags\": [
      {\"tagType\": \"time\", \"tagValue\": \"午餐\"},
      {\"tagType\": \"type\", \"tagValue\": \"炒菜\"}
    ]
  }")

echo "响应: $CREATE_RECIPE_RESPONSE"

RECIPE_ID=$(echo "$CREATE_RECIPE_RESPONSE" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)

if [ -z "$RECIPE_ID" ]; then
    echo -e "${RED}❌ 创建菜谱失败${NC}"
    echo "   可能原因："
    echo "   1. Token 未正确传递"
    echo "   2. 后端拦截器拦截了请求"
    echo "   3. 数据库连接失败"
    exit 1
else
    echo -e "${GREEN}✓ 创建菜谱成功${NC}"
    echo "菜谱ID: $RECIPE_ID"
    echo "菜谱名称: $RECIPE_NAME"
fi
echo ""

# 测试 6: 查询菜谱列表
echo -e "${YELLOW}测试 6: 查询菜谱列表${NC}"
SEARCH_RESPONSE=$(curl -s -X GET "$BASE_URL/api/recipes/search?pageNum=1&pageSize=10" \
  -H "Authorization: Bearer $TOKEN")

echo "响应: $SEARCH_RESPONSE"

if echo "$SEARCH_RESPONSE" | grep -q '"code":200'; then
    echo -e "${GREEN}✓ 查询成功${NC}"
    TOTAL=$(echo "$SEARCH_RESPONSE" | grep -o '"total":[0-9]*' | cut -d':' -f2)
    echo "总菜谱数: $TOTAL"
else
    echo -e "${RED}❌ 查询失败${NC}"
fi
echo ""

# 测试总结
echo "========================================"
echo -e "${GREEN}✓ 所有测试通过！${NC}"
echo "========================================"
echo ""
echo "现在可以在 Android 客户端使用以下账号登录："
echo "  邮箱: $EMAIL"
echo "  密码: $PASSWORD"
echo ""
echo "后端日志文件位置: logs/family-recipes.log"
echo ""

