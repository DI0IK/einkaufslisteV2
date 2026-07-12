package main

// OldShoppingList represents the ShoppingList table in the old database version.
type OldShoppingList struct {
	ID        string `gorm:"column:id"`
	Name      string `gorm:"column:name"`
	CreatedAt int64  `gorm:"column:createdAt"`
	UpdatedAt int64  `gorm:"column:updatedAt"`
}

func (OldShoppingList) TableName() string {
	return "ShoppingList"
}

// OldAvailableItem represents the AvailableItem table in the old database version.
type OldAvailableItem struct {
	ID             string  `gorm:"column:id"`
	Name           string  `gorm:"column:name"`
	Unit           *string `gorm:"column:unit"`
	Category       *string `gorm:"column:category"`
	ShoppingListID string  `gorm:"column:shoppingListId"`
	CreatedAt      int64   `gorm:"column:createdAt"`
	UpdatedAt      int64   `gorm:"column:updatedAt"`
}

func (OldAvailableItem) TableName() string {
	return "AvailableItem"
}

// OldShoppingListItem represents the ShoppingListItem table in the old database version.
type OldShoppingListItem struct {
	ID             string  `gorm:"column:id"`
	ItemID         string  `gorm:"column:itemId"`
	ShoppingListID string  `gorm:"column:shoppingListId"`
	Quantity       float64 `gorm:"column:quantity"`
	Checked        bool    `gorm:"column:checked"`
	ItemPrefix     *string `gorm:"column:itemPrefix"`
	CreatedAt      int64   `gorm:"column:createdAt"`
	UpdatedAt      int64   `gorm:"column:updatedAt"`
}

func (OldShoppingListItem) TableName() string {
	return "ShoppingListItem"
}
