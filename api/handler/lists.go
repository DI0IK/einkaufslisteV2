package handler

import (
	"net/http"
	"time"

	"github.com/gin-gonic/gin"
	"github.com/DI0IK/einkaufslisteV2/api/model"
	"gorm.io/gorm"
)

func ListShoppingLists(db *gorm.DB) gin.HandlerFunc {
	return func(c *gin.Context) {
		updatedSinceStr := c.Query("updatedSince")
		var lists []model.ShoppingList
		query := db.Model(&model.ShoppingList{})

		if updatedSinceStr != "" {
			if updatedSince, err := time.Parse(time.RFC3339, updatedSinceStr); err == nil {
				query = query.Unscoped().Where("updated_at >= ? OR deleted_at >= ?", updatedSince, updatedSince)
			}
		}

		if err := query.Find(&lists).Error; err != nil {
			c.JSON(http.StatusInternalServerError, gin.H{"success": false, "message": "Database error"})
			return
		}

		c.JSON(http.StatusOK, gin.H{"success": true, "data": lists})
	}
}

func CreateShoppingList(db *gorm.DB) gin.HandlerFunc {
	return func(c *gin.Context) {
		var req model.ShoppingList
		if err := c.ShouldBindJSON(&req); err != nil {
			c.JSON(http.StatusBadRequest, gin.H{"success": false, "message": "Invalid request"})
			return
		}

		if err := db.Create(&req).Error; err != nil {
			c.JSON(http.StatusInternalServerError, gin.H{"success": false, "message": "Failed to create list"})
			return
		}

		c.JSON(http.StatusCreated, gin.H{"success": true, "data": req})
	}
}

func UpdateShoppingList(db *gorm.DB) gin.HandlerFunc {
	return func(c *gin.Context) {
		listID := c.Param("listId")
		
		var updateData map[string]any
		if err := c.ShouldBindJSON(&updateData); err != nil {
			c.JSON(http.StatusBadRequest, gin.H{"success": false, "message": "Invalid payload"})
			return
		}

		delete(updateData, "id")

		result := db.Model(&model.ShoppingList{}).Where("id = ?", listID).Updates(updateData)
		if result.Error != nil {
			c.JSON(http.StatusInternalServerError, gin.H{"success": false, "message": "Update failed: " + result.Error.Error()})
			return
		}
		if result.RowsAffected == 0 {
			c.JSON(http.StatusNotFound, gin.H{"success": false, "message": "List not found"})
			return
		}

		var updatedList model.ShoppingList
		db.First(&updatedList, "id = ?", listID)
		c.JSON(http.StatusOK, gin.H{"success": true, "data": updatedList})
	}
}

func DeleteShoppingList(db *gorm.DB) gin.HandlerFunc {
	return func(c *gin.Context) {
		listID := c.Param("listId")
		
		result := db.Where("id = ?", listID).Delete(&model.ShoppingList{})
		if result.Error != nil {
			c.JSON(http.StatusInternalServerError, gin.H{"success": false, "message": "Delete failed"})
			return
		}
		
		c.Status(http.StatusNoContent)
	}
}
