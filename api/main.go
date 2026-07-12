package main

import (
	"log"
	"os"

	"github.com/gin-gonic/gin"
	"github.com/DI0IK/einkaufslisteV2/api/handler"
	"github.com/DI0IK/einkaufslisteV2/api/middleware"
	"github.com/DI0IK/einkaufslisteV2/api/store"
)

func main() {
	db := store.ConnectDB()
	r := gin.Default()

	issuer := os.Getenv("OIDC_ISSUER_URL")
	if issuer == "" {
		issuer = "https://sso.dominikstahl.dev/application/o/einkaufsliste-pub/"
	}
	clientID := os.Getenv("OIDC_CLIENT_ID")
	if clientID == "" {
		clientID = "zDHMQhYvPGeLH2mg5Jq8qWr9kI6WH5QAMdKRoNFG"
	}

	api := r.Group("/api")
	api.Use(middleware.AuthMiddleware(issuer, clientID))
	
	lists := api.Group("/shopping-lists")
	{
		lists.GET("", handler.ListShoppingLists(db))
		lists.POST("", handler.CreateShoppingList(db))
		lists.PATCH("/:listId", handler.UpdateShoppingList(db))
		lists.DELETE("/:listId", handler.DeleteShoppingList(db))

		// Catalog
		lists.GET("/:listId/catalog", handler.ListAvailableItems(db))
		lists.POST("/:listId/catalog", handler.CreateAvailableItem(db))
		lists.PATCH("/:listId/catalog/:itemId", handler.UpdateAvailableItem(db))
		lists.DELETE("/:listId/catalog/:itemId", handler.DeleteAvailableItem(db))

		// Active List Items
		lists.GET("/:listId/items", handler.ListShoppingListItems(db))
		lists.POST("/:listId/items", handler.CreateShoppingListItem(db))
		lists.PATCH("/:listId/items/:listItemId", handler.UpdateShoppingListItem(db))
		lists.DELETE("/:listId/items/:listItemId", handler.DeleteShoppingListItem(db))
	}

	port := os.Getenv("PORT")
	if port == "" {
		port = "8080"
	}

	log.Printf("Starting server on port %s", port)
	r.Run(":" + port)
}