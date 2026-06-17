# --- Build stage ---
# Build the Flutter Web bundle.
FROM ghcr.io/cirruslabs/flutter:stable AS build
WORKDIR /app
# Copy the lockfile too so dependency versions are pinned and reproducible
# (prevents transitive analyzer/source_gen drift from breaking codegen).
COPY pubspec.yaml pubspec.lock ./
RUN flutter pub get
COPY . .
# Generated freezed / json_serializable sources are committed; refresh them so
# the bundle always matches the current models, then build the web bundle.
RUN dart run build_runner build --delete-conflicting-outputs
RUN flutter build web --release

# --- Runtime stage ---
FROM nginx:1.27-alpine
COPY --from=build /app/build/web /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
