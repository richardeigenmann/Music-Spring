# Stage 1: Build the Angular application
FROM node:20 AS build
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
RUN npm run build

# Stage 2: Serve the application with Express
FROM node:20-slim
WORKDIR /app
COPY --from=build /app/dist ./dist
COPY server.js ./
COPY package*.json ./
RUN npm install --omit=dev

# Environment variable for the backend URL (overridable at runtime)
ENV BACKEND_URL=http://localhost:8002
ENV PORT=4200

EXPOSE 4200
CMD ["node", "server.js"]
