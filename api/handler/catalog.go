package handler

import (
	"net/http"
	"time"

	"github.com/gin-gonic/gin"
	"github.com/DI0IK/einkaufslisteV2/api/model"
	"gorm.io/gorm"
)

func ListAvailableItems(db *gorm.DB) gin.HandlerFunc {
	return func(c *gin.Context) {
		listID := c.Param("listId")
		updatedSinceStr := c.Query("updatedSince")

		var items []model.AvailableItem
		query := db.Model(&model.AvailableItem{}).Where("list_id = ?", listID)

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

func CreateAvailableItem(db *gorm.DB) gin.HandlerFunc {
	return func(c *gin.Context) {
		listID := c.Param("listId")
		
		var req model.AvailableItem
		if err := c.ShouldBindJSON(&req); err != nil {
			c.JSON(http.StatusBadRequest, gin.H{"success": false, "message": "Invalid request"})
			return
		}

		req.ListID = listID

		if err := db.Create(&req).Error; err != nil {
			c.JSON(http.StatusInternalServerError, gin.H{"success": false, "message": "Failed to create catalog item"})
			return
		}

		c.JSON(http.StatusCreated, gin.H{"success": true, "data": req})
	}
}

func UpdateAvailableItem(db *gorm.DB) gin.HandlerFunc {
	return func(c *gin.Context) {
		listID := c.Param("listId")
		itemID := c.Param("itemId")

		var updateData map[string]any
		if err := c.ShouldBindJSON(&updateData); err != nil {
			c.JSON(http.StatusBadRequest, gin.H{"success": false, "message": "Invalid payload"})
			return
		}

		delete(updateData, "id")
		delete(updateData, "listId")

		result := db.Model(&model.AvailableItem{}).
			Where("id = ? AND list_id = ?", itemID, listID).
			Updates(updateData)

		if result.Error != nil {
			c.JSON(http.StatusInternalServerError, gin.H{"success": false, "message": "Update failed: " + result.Error.Error()})
			return
		}
		if result.RowsAffected == 0 {
			c.JSON(http.StatusNotFound, gin.H{"success": false, "message": "Item not found"})
			return
		}

		var updatedItem model.AvailableItem
		db.First(&updatedItem, "id = ?", itemID)
		c.JSON(http.StatusOK, gin.H{"success": true, "data": updatedItem})
	}
}

func DeleteAvailableItem(db *gorm.DB) gin.HandlerFunc {
	return func(c *gin.Context) {
		listID := c.Param("listId")
		itemID := c.Param("itemId")

		result := db.Where("id = ? AND list_id = ?", itemID, listID).Delete(&model.AvailableItem{})
		if result.Error != nil {
			c.JSON(http.StatusInternalServerError, gin.H{"success": false, "message": "Delete failed"})
			return
		}

		c.Status(http.StatusNoContent)
	}
}
