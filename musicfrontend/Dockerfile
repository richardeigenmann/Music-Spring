# Stage 1: Build the Angular application
FROM node:22 AS build
RUN corepack enable && corepack prepare pnpm@latest --activate
WORKDIR /app
COPY package.json pnpm-lock.yaml pnpm-workspace.yaml* ./
RUN pnpm install --frozen-lockfile
COPY . .
RUN pnpm run build

# Stage 2: Serve the application with Express
FROM node:22-slim
RUN corepack enable && corepack prepare pnpm@latest --activate
WORKDIR /app
COPY --from=build /app/dist ./dist
COPY server.js ./
COPY package.json pnpm-lock.yaml pnpm-workspace.yaml* ./
RUN pnpm install --prod --frozen-lockfile

# Environment variable for the backend URL (overridable at runtime)
ENV BACKEND_URL=http://localhost:8002
ENV PORT=4200

EXPOSE 4200
CMD ["node", "server.js"]
