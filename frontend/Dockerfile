# --- Build stage ---
# Build the Flutter Web bundle.
FROM ghcr.io/cirruslabs/flutter:stable AS build
WORKDIR /app
COPY pubspec.yaml ./
RUN flutter pub get
COPY . .
# Generate freezed / json_serializable sources, then build the web bundle.
RUN dart run build_runner build --delete-conflicting-outputs
RUN flutter build web --release

# --- Runtime stage ---
FROM nginx:1.27-alpine
COPY --from=build /app/build/web /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
