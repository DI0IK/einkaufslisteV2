package main

import (
	"fmt"
	"log"
	"time"

	"github.com/DI0IK/einkaufslisteV2/api/model"
	"gorm.io/gorm"
	"gorm.io/gorm/clause"
)

// RunMigration reads from srcDB and writes mapped entries to targetDB.
func RunMigration(srcDB, targetDB *gorm.DB) error {
	// 1. Migrate ShoppingLists
	log.Println("Migrating Shopping Lists...")
	var oldLists []OldShoppingList
	if err := srcDB.Find(&oldLists).Error; err != nil {
		return fmt.Errorf("failed to fetch old shopping lists: %w", err)
	}

	for _, oldList := range oldLists {
		defaultIcon := "shopping_cart"
		defaultColor := "#8B5CF6"
		newList := model.ShoppingList{
			BaseModel: model.BaseModel{
				ID:        oldList.ID,
				CreatedAt: time.UnixMilli(oldList.CreatedAt),
				UpdatedAt: time.UnixMilli(oldList.UpdatedAt),
			},
			Name:  oldList.Name,
			Icon:  &defaultIcon,
			Color: &defaultColor,
		}

		err := targetDB.Clauses(clause.OnConflict{
			UpdateAll: true,
		}).Create(&newList).Error
		if err != nil {
			log.Printf("Failed to migrate shopping list %s (%s): %v", oldList.Name, oldList.ID, err)
		}
	}
	log.Printf("Migrated %d shopping lists.", len(oldLists))

	// 2. Migrate AvailableItems
	log.Println("Migrating Available Items...")
	var oldItems []OldAvailableItem
	if err := srcDB.Find(&oldItems).Error; err != nil {
		return fmt.Errorf("failed to fetch old available items: %w", err)
	}

	oldItemUnits := make(map[string]*string)

	for _, oldItem := range oldItems {
		oldItemUnits[oldItem.ID] = oldItem.Unit

		newItem := model.AvailableItem{
			BaseModel: model.BaseModel{
				ID:        oldItem.ID,
				CreatedAt: time.UnixMilli(oldItem.CreatedAt),
				UpdatedAt: time.UnixMilli(oldItem.UpdatedAt),
			},
			ListID:   oldItem.ShoppingListID,
			Name:     oldItem.Name,
			Category: oldItem.Category,
		}

		err := targetDB.Clauses(clause.OnConflict{
			UpdateAll: true,
		}).Create(&newItem).Error
		if err != nil {
			log.Printf("Failed to migrate available item %s (%s): %v", oldItem.Name, oldItem.ID, err)
		}
	}
	log.Printf("Migrated %d available items.", len(oldItems))

	// 3. Migrate ShoppingListItems
	log.Println("Migrating Shopping List Items...")
	var oldListItems []OldShoppingListItem
	if err := srcDB.Find(&oldListItems).Error; err != nil {
		return fmt.Errorf("failed to fetch old shopping list items: %w", err)
	}

	for _, oldListItem := range oldListItems {
		var unit *string
		if u, ok := oldItemUnits[oldListItem.ItemID]; ok {
			unit = u
		}

		addedBy := "system_import"

		newListItem := model.ShoppingListItem{
			BaseModel: model.BaseModel{
				ID:        oldListItem.ID,
				CreatedAt: time.UnixMilli(oldListItem.CreatedAt),
				UpdatedAt: time.UnixMilli(oldListItem.UpdatedAt),
			},
			ItemID:    oldListItem.ItemID,
			ListID:    oldListItem.ShoppingListID,
			Quantity:  oldListItem.Quantity,
			Checked:   oldListItem.Checked,
			Unit:      unit,
			Note:      oldListItem.ItemPrefix,
			AddedBy:   &addedBy,
			SortOrder: 0,
		}

		err := targetDB.Clauses(clause.OnConflict{
			UpdateAll: true,
		}).Create(&newListItem).Error
		if err != nil {
			log.Printf("Failed to migrate shopping list item %s: %v", oldListItem.ID, err)
		}
	}
	log.Printf("Migrated %d shopping list items.", len(oldListItems))

	return nil
}
