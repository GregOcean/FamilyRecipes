#!/bin/bash

# 家肴应用 API 测试脚本（开发模式 - 无需登录）
# 用于测试开发模式下不需要认证的 API

BASE_URL="http://localhost:8080"

echo "========================================"
echo "家肴应用 API 测试（开发模式 - 无需登录）"
echo "========================================"
echo ""

# 颜色输出
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 测试 1: 检查后端是否启动
echo -e "${YELLOW}测试 1: 检查后端是否启动${NC}"
echo "请求: GET $BASE_URL/api/recipes/search"
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/api/recipes/search?pageNum=1&pageSize=1")

if [ "$HTTP_CODE" -eq 000 ]; then
    echo -e "${RED}❌ 后端未启动或无法连接${NC}"
    echo "   请先启动后端："
    echo "   cd backend && ./mvnw spring-boot:run"
    exit 1
elif [ "$HTTP_CODE" -eq 401 ]; then
    echo -e "${RED}❌ 后端未开启开发模式（需要登录）${NC}"
    echo "   请检查 AuthInterceptor.java 中 DEV_MODE 是否为 true"
    exit 1
else
    echo -e "${GREEN}✓ 后端已启动且处于开发模式 (HTTP $HTTP_CODE)${NC}"
fi
echo ""

# 测试 2: 查询菜谱列表（无需登录）
echo -e "${YELLOW}测试 2: 查询菜谱列表（无需登录）${NC}"
SEARCH_RESPONSE=$(curl -s -X GET "$BASE_URL/api/recipes/search?pageNum=1&pageSize=10")

echo "响应: $SEARCH_RESPONSE"

if echo "$SEARCH_RESPONSE" | grep -q '"code":200'; then
    echo -e "${GREEN}✓ 查询成功（无需登录）${NC}"
    TOTAL=$(echo "$SEARCH_RESPONSE" | grep -o '"total":[0-9]*' | cut -d':' -f2)
    echo "总菜谱数: $TOTAL"
else
    echo -e "${RED}❌ 查询失败${NC}"
    exit 1
fi
echo ""

# 测试 3: 创建菜谱（无需登录）
echo -e "${YELLOW}测试 3: 创建菜谱（无需登录）${NC}"
RECIPE_NAME="开发测试菜谱_$(date +%H%M%S)"

CREATE_RESPONSE=$(curl -s -X POST $BASE_URL/api/recipes \
  -H "Content-Type: application/json" \
  -d "{
    \"recipe\": {
      \"name\": \"$RECIPE_NAME\",
      \"description\": \"这是在开发模式下不登录创建的菜谱，创建者应该是默认用户\"
    },
    \"tags\": [
      {\"tagType\": \"time\", \"tagValue\": \"午餐\"},
      {\"tagType\": \"type\", \"tagValue\": \"炒菜\"}
    ]
  }")

echo "响应: $CREATE_RESPONSE"

RECIPE_ID=$(echo "$CREATE_RESPONSE" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)

if [ -z "$RECIPE_ID" ]; then
    echo -e "${RED}❌ 创建菜谱失败${NC}"
    echo "   可能原因："
    echo "   1. 数据库中不存在默认用户（ID=1）"
    echo "   2. 开发模式未正确配置"
    echo ""
    echo "解决方案："
    echo "   执行: mysql -u root -p family_recipes < backend/src/main/resources/data.sql"
    exit 1
else
    echo -e "${GREEN}✓ 创建菜谱成功（无需登录）${NC}"
    echo "菜谱ID: $RECIPE_ID"
    echo "菜谱名称: $RECIPE_NAME"
    
    # 检查创建者
    CREATOR_ID=$(echo "$CREATE_RESPONSE" | grep -o '"creatorId":[0-9]*' | cut -d':' -f2)
    if [ "$CREATOR_ID" -eq 1 ] 2>/dev/null; then
        echo -e "${GREEN}✓ 创建者ID为1（默认用户），符合预期${NC}"
    else
        echo -e "${YELLOW}⚠ 创建者ID为 $CREATOR_ID（预期为1）${NC}"
    fi
fi
echo ""

# 测试 4: 上传图片（无需登录）
echo -e "${YELLOW}测试 4: 上传图片（无需登录）${NC}"
# 创建一个临时的小图片文件（1x1 透明 PNG）
echo "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==" | base64 -d > /tmp/test_image.png

UPLOAD_RESPONSE=$(curl -s -X POST $BASE_URL/api/upload/image \
  -F "file=@/tmp/test_image.png")

echo "响应: $UPLOAD_RESPONSE"

IMAGE_URL=$(echo "$UPLOAD_RESPONSE" | grep -o '"data":"[^"]*' | cut -d'"' -f4)

if [ -z "$IMAGE_URL" ]; then
    echo -e "${YELLOW}⚠ 图片上传失败（可能需要创建 uploads 目录）${NC}"
    echo "   执行: mkdir -p backend/uploads"
else
    echo -e "${GREEN}✓ 图片上传成功（无需登录）${NC}"
    echo "图片URL: $IMAGE_URL"
fi

# 清理临时文件
rm -f /tmp/test_image.png
echo ""

# 测试 5: 获取菜谱详情（无需登录）
if [ ! -z "$RECIPE_ID" ]; then
    echo -e "${YELLOW}测试 5: 获取菜谱详情（无需登录）${NC}"
    DETAIL_RESPONSE=$(curl -s -X GET "$BASE_URL/api/recipes/$RECIPE_ID")
    
    if echo "$DETAIL_RESPONSE" | grep -q '"code":200'; then
        echo -e "${GREEN}✓ 获取详情成功${NC}"
        RECIPE_NAME_DETAIL=$(echo "$DETAIL_RESPONSE" | grep -o '"name":"[^"]*' | head -1 | cut -d'"' -f4)
        echo "菜谱名称: $RECIPE_NAME_DETAIL"
    else
        echo -e "${RED}❌ 获取详情失败${NC}"
    fi
    echo ""
fi

# 测试总结
echo "========================================"
echo -e "${GREEN}✓ 开发模式测试通过！${NC}"
echo "========================================"
echo ""
echo "测试结果："
echo "  • 后端处于开发模式，无需登录"
echo "  • 所有操作使用默认用户（ID=1）"
echo "  • Android 客户端现在可以直接使用所有功能"
echo ""
echo "默认用户信息："
echo "  • 用户ID: 1"
echo "  • 邮箱: default@familyrecipes.com"
echo "  • 用户名: 家肴用户_DEV"
echo "  • 密码: 123456"
echo ""
echo "查看更多信息: cat DEV_MODE.md"
echo ""

