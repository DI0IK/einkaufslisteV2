package handler

import (
	"net/http"
	"time"

	"github.com/gin-gonic/gin"
	"github.com/DI0IK/einkaufslisteV2/api/model"
	"gorm.io/gorm"
)

func ListShoppingListItems(db *gorm.DB) gin.HandlerFunc {
	return func(c *gin.Context) {
		listID := c.Param("listId")
		updatedSinceStr := c.Query("updatedSince")

		var items []model.ShoppingListItem
		query := db.Preload("Item").Where("list_id = ?", listID)

		if updatedSinceStr != "" {
			if updatedSince, err := time.Parse(time.RFC3339, updatedSinceStr); err == nil {
				query = query.Unscoped().Where("updated_at >= ? OR deleted_at >= ?", updatedSince, updatedSince)
			}
		}

		if err := query.Find(&items).Error; err != nil {
			c.JSON(http.StatusInternalServerError, gin.H{"success": false, "message": "Database error"})
			return
		}

		c.JSON(http.StatusOK, gin.H{"success": true, "data": items})
	}
}

func CreateShoppingListItem(db *gorm.DB) gin.HandlerFunc {
	type createReq struct {
		ID        string  `json:"id"`
		ItemID    string  `json:"itemId" binding:"required"`
		Quantity  float64 `json:"quantity"`
		Unit      *string `json:"unit"`
		Checked   bool    `json:"checked"`
		SortOrder int     `json:"sortOrder"`
		Note      *string `json:"note"`
	}

	return func(c *gin.Context) {
		listID := c.Param("listId")

		var req createReq
		if err := c.ShouldBindJSON(&req); err != nil {
			c.JSON(http.StatusBadRequest, gin.H{"success": false, "message": "Invalid request payload"})
			return
		}

		var usernamePtr *string
		if username, exists := c.Get("username"); exists {
			if uStr, ok := username.(string); ok {
				usernamePtr = &uStr
			}
		}

		listItem := model.ShoppingListItem{
			ListID:    listID,
			ItemID:    req.ItemID,
			Quantity:  req.Quantity,
			Unit:      req.Unit,
			Checked:   req.Checked,
			SortOrder: req.SortOrder,
			Note:      req.Note,
			AddedBy:   usernamePtr,
		}
		
		if req.ID != "" {
			listItem.ID = req.ID
		}

		if err := db.Create(&listItem).Error; err != nil {
			c.JSON(http.StatusInternalServerError, gin.H{"success": false, "message": "Failed to add item to list"})
			return
		}

		db.Preload("Item").First(&listItem, "id = ?", listItem.ID)

		c.JSON(http.StatusCreated, gin.H{"success": true, "data": listItem})
	}
}

func UpdateShoppingListItem(db *gorm.DB) gin.HandlerFunc {
	return func(c *gin.Context) {
		listID := c.Param("listId")
		listItemID := c.Param("listItemId")

		var updateData map[string]any
		if err := c.ShouldBindJSON(&updateData); err != nil {
			c.JSON(http.StatusBadRequest, gin.H{"success": false, "message": "Invalid payload"})
			return
		}

		delete(updateData, "id")
		delete(updateData, "listId")
		delete(updateData, "itemId")

		result := db.Model(&model.ShoppingListItem{}).
			Where("id = ? AND list_id = ?", listItemID, listID).
			Updates(updateData)

		if result.Error != nil {
			c.JSON(http.StatusInternalServerError, gin.H{"success": false, "message": "Update failed: " + result.Error.Error()})
			return
		}
		if result.RowsAffected == 0 {
			c.JSON(http.StatusNotFound, gin.H{"success": false, "message": "List item not found"})
			return
		}

		var updatedItem model.ShoppingListItem
		db.Preload("Item").First(&updatedItem, "id = ?", listItemID)
		c.JSON(http.StatusOK, gin.H{"success": true, "data": updatedItem})
	}
}

func DeleteShoppingListItem(db *gorm.DB) gin.HandlerFunc {
	return func(c *gin.Context) {
		listID := c.Param("listId")
		listItemID := c.Param("listItemId")

		result := db.Where("id = ? AND list_id = ?", listItemID, listID).Delete(&model.ShoppingListItem{})
		if result.Error != nil {
			c.JSON(http.StatusInternalServerError, gin.H{"success": false, "message": "Delete failed"})
			return
		}

		c.Status(http.StatusNoContent)
	}
}
