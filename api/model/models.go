package model

import (
	"time"

	"github.com/google/uuid"
	"gorm.io/gorm"
)

type BaseModel struct {
	ID        string         `gorm:"type:text;primaryKey" json:"id"`
	CreatedAt time.Time      `json:"createdAt"`
	UpdatedAt time.Time      `json:"updatedAt"`
	DeletedAt gorm.DeletedAt `gorm:"index" json:"deletedAt,omitempty"`
}

func (base *BaseModel) BeforeCreate(tx *gorm.DB) (err error) {
	if base.ID == "" {
		base.ID = uuid.NewString()
	}
	return
}

type ShoppingList struct {
	BaseModel
	Name  string  `gorm:"not null" json:"name"`
	Icon  *string `json:"icon"`
	Color *string `json:"color"`
}

type AvailableItem struct {
	BaseModel
	ListID   string  `gorm:"type:text;not null;index" json:"-"`
	Name     string  `gorm:"not null" json:"name"`
	Category *string `json:"category"`
}

type ShoppingListItem struct {
	BaseModel
	Item      AvailableItem `gorm:"foreignKey:ItemID" json:"item"`
	ItemID    string        `gorm:"type:text;not null" json:"-"`
	ListID    string        `gorm:"type:text;not null;index" json:"-"`
	Quantity  float64       `gorm:"not null" json:"quantity"`
	Unit      *string       `json:"unit"`
	Checked   bool          `gorm:"default:false" json:"checked"`
	SortOrder int           `gorm:"default:0" json:"sortOrder"`
	Note      *string       `json:"note"`
	AddedBy   *string       `json:"addedBy"`
}