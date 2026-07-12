package middleware

import (
	"context"
	"net/http"
	"strings"
	"sync"
	"log"

	"github.com/coreos/go-oidc/v3/oidc"
	"github.com/gin-gonic/gin"
)

var (
	provider  *oidc.Provider
	verifier  *oidc.IDTokenVerifier
	initOnce  sync.Once
	initError error
)

func initOIDC(ctx context.Context, issuerURL, clientID string) {
	initOnce.Do(func() {
		log.Printf("Initializing OIDC Provider with Issuer: %s, Client ID: %s", issuerURL, clientID)
		prov, err := oidc.NewProvider(ctx, issuerURL)
		if err != nil {
			initError = err
			log.Printf("OIDC initialization failed: %v", err)
			return
		}
		provider = prov
		verifier = provider.Verifier(&oidc.Config{ClientID: clientID})
	})
}

func AuthMiddleware(issuerURL, clientID string) gin.HandlerFunc {
	// Initialize using background context so background JWKS key refreshes don't fail when request context cancels
	initOIDC(context.Background(), issuerURL, clientID)

	return func(c *gin.Context) {
		if initError != nil {
			c.JSON(http.StatusInternalServerError, gin.H{"success": false, "message": "Authentication service initialization failed: " + initError.Error()})
			c.Abort()
			return
		}

		authHeader := c.GetHeader("Authorization")
		if authHeader == "" {
			c.JSON(http.StatusUnauthorized, gin.H{"success": false, "message": "Authorization header missing"})
			c.Abort()
			return
		}

		parts := strings.SplitN(authHeader, " ", 2)
		if len(parts) != 2 || strings.ToLower(parts[0]) != "bearer" {
			c.JSON(http.StatusUnauthorized, gin.H{"success": false, "message": "Authorization header format must be Bearer {token}"})
			c.Abort()
			return
		}

		tokenString := parts[1]
		idToken, err := verifier.Verify(c.Request.Context(), tokenString)
		if err != nil {
			c.JSON(http.StatusUnauthorized, gin.H{"success": false, "message": "Invalid or expired token: " + err.Error()})
			c.Abort()
			return
		}

		// Extract standard OIDC claims
		var claims struct {
			Subject           string `json:"sub"`
			Email             string `json:"email"`
			PreferredUsername string `json:"preferred_username"`
		}
		if err := idToken.Claims(&claims); err != nil {
			c.JSON(http.StatusUnauthorized, gin.H{"success": false, "message": "Failed to parse token claims"})
			c.Abort()
			return
		}

		// Pass the username (fallback to email/subject) to the handlers
		username := claims.PreferredUsername
		if username == "" {
			username = claims.Email
		}
		if username == "" {
			username = claims.Subject
		}

		c.Set("username", username)
		c.Next()
	}
}
