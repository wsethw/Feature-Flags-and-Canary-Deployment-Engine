FROM node:20-alpine
WORKDIR /app

RUN apk add --no-cache wget

COPY package*.json ./
RUN npm ci --omit=dev

COPY server.js .
COPY lib ./lib

EXPOSE 8080

USER node
CMD ["npm", "start"]
