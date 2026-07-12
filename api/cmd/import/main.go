package main

import (
	"flag"
	"log"

	"github.com/glebarez/sqlite"
	"github.com/DI0IK/einkaufslisteV2/api/model"
	"gorm.io/gorm"
)

func main() {
	sourcePath := flag.String("source", "../import.db", "Path to source SQLite database")
	targetPath := flag.String("target", "einkaufsliste.db", "Path to target SQLite database")
	flag.Parse()

	log.Printf("Connecting to source database: %s", *sourcePath)
	srcDB, err := gorm.Open(sqlite.Open(*sourcePath), &gorm.Config{})
	if err != nil {
		log.Fatalf("Failed to open source database: %v", err)
	}

	log.Printf("Connecting to target database: %s", *targetPath)
	dsn := *targetPath + "?_pragma=journal_mode(WAL)&_pragma=foreign_keys(1)"
	targetDB, err := gorm.Open(sqlite.Open(dsn), &gorm.Config{})
	if err != nil {
		log.Fatalf("Failed to open target database: %v", err)
	}

	// AutoMigrate target schema to ensure tables exist
	log.Println("Ensuring target database is migrated...")
	err = targetDB.AutoMigrate(
		&model.ShoppingList{},
		&model.AvailableItem{},
		&model.ShoppingListItem{},
	)
	if err != nil {
		log.Fatalf("Failed to migrate target database: %v", err)
	}

	// Run migration logic from importer.go
	if err := RunMigration(srcDB, targetDB); err != nil {
		log.Fatalf("Migration failed: %v", err)
	}

	log.Println("Database migration completed successfully!")
}
