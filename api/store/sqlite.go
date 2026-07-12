package store

import (
	"log"
	"os"

	"github.com/glebarez/sqlite"
	"github.com/DI0IK/einkaufslisteV2/api/model"
	"gorm.io/gorm"
)

func ConnectDB() *gorm.DB {
	dbPath := os.Getenv("DATABASE_PATH")
	if dbPath == "" {
		dbPath = "einkaufsliste.db"
	}

	dsn := dbPath + "?_pragma=journal_mode(WAL)&_pragma=foreign_keys(1)"

	db, err := gorm.Open(sqlite.Open(dsn), &gorm.Config{})
	if err != nil {
		log.Fatalf("Failed to connect to database: %v", err)
	}

	err = db.AutoMigrate(
		&model.ShoppingList{},
		&model.AvailableItem{},
		&model.ShoppingListItem{},
	)
	if err != nil {
		log.Fatalf("Failed to migrate database: %v", err)
	}

	return db
}