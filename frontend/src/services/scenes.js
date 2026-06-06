export const presetScenes = [
  {
    id: 'order_food',
    name: 'Diner Order',
    place: 'Star Bites diner',
    goal: 'Order drinks, choose food, ask for details, and keep the exchange natural.',
    opening: 'The waiter greets you at a busy American diner and asks what you would like to drink.'
  },
  {
    id: 'check_in',
    name: 'Airport Check-in',
    place: 'Global Airways counter',
    goal: 'Share booking details, talk about baggage, and answer check-in questions politely.',
    opening: 'The ground crew agent asks for your passport and booking details.'
  },
  {
    id: 'interview',
    name: 'Tech Interview',
    place: 'TechGen hiring room',
    goal: 'Introduce yourself, explain experience, and answer one interview question at a time.',
    opening: 'The HR manager introduces the interview and asks the first question.'
  },
  {
    id: 'meeting',
    name: 'Launch Meeting',
    place: 'Mobile app launch sync',
    goal: 'Give a short progress update, discuss roadblocks, and agree on next steps.',
    opening: 'The project manager welcomes you and asks for a quick task status update.'
  }
]

export const customSceneTemplate = {
  id: 'custom',
  name: 'Custom Scene',
  place: 'Your own roleplay',
  goal: 'Describe the role, situation, and speaking challenge you want to practice.',
  opening: 'Create a roleplay prompt below, then record your first message.'
}

export const scenes = [...presetScenes, customSceneTemplate]
