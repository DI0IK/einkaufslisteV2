# Build Stage
FROM docker.io/library/golang:1.26-alpine AS builder

WORKDIR /app

# Copy dependency manifests
COPY go.mod go.sum ./
RUN go mod download

# Copy source code
COPY . .

# Build the application statically with CGO disabled
RUN CGO_ENABLED=0 GOOS=linux go build -o server main.go

# Production Stage
FROM docker.io/library/alpine:latest

# Install CA certificates for secure OIDC provider connection
RUN apk --no-cache add ca-certificates

WORKDIR /app

# Create a non-root group/user for security
RUN addgroup -S app && adduser -S app -G app \
    && chown -R app:app /app

USER app

# Copy the compiled binary
COPY --from=builder --chown=app:app /app/server .

# Default configuration environment variables
ENV PORT=8080
ENV GIN_MODE=release
EXPOSE 8080

CMD ["./server"]
